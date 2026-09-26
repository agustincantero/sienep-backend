package com.fides.sienep.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Facade sobre ActiveDirectoryLdapAuthenticationProvider, misma forma que GoogleIdTokenVerifierService: recibe credenciales y devuelve solo el email ya verificado por el controlador de dominio — AuthService no necesita conocer nada de LDAP. A diferencia de Google, acá SÍ hay que distinguir "credenciales rechazadas" (401) de "AD no configurado o controlador de dominio inalcanzable" (503): lo primero son intentos de login normales, lo segundo es un problema de infraestructura que no debería disfrazarse de contraseña incorrecta.
@Service
public class ActiveDirectoryVerifierService {

    private static final Logger log = LoggerFactory.getLogger(ActiveDirectoryVerifierService.class);

    // Timeouts del lado de LDAP: sin ellos, un controlador de dominio caído deja la conexión (y con ella el proxy del frontend) esperando indefinidamente.
    private static final String LDAP_TIMEOUT_MS = "5000";

    // Búsqueda por sAMAccountName (variante C de la propuesta): {1} es el usuario tal como lo escribe la persona (sin dominio), {0} es el nombre de login con el que se hizo el bind (usuario@dominio). No se exige un UPN igual al email.
    private static final String SEARCH_FILTER = "(&(objectClass=user)(sAMAccountName={1}))";

    @Value("${ad.ldap.url:}")
    private String ldapUrl;

    @Value("${ad.domain:}")
    private String domain;

    @Value("${ad.root-dn:}")
    private String rootDn;

    // Construido una sola vez (lazy) y reutilizado, mismo criterio que el GoogleIdTokenVerifier de GoogleIdTokenVerifierService.
    private ActiveDirectoryLdapAuthenticationProvider provider;

    public Optional<String> emailVerificado(String usuario, String contrasenia) {
        return autenticar(obtenerProvider(), usuario, contrasenia);
    }

    // Separado de emailVerificado() para poder ejercer la lógica de autenticación en tests con un proveedor simulado, sin necesidad de un AD real.
    Optional<String> autenticar(ActiveDirectoryLdapAuthenticationProvider provider, String usuario, String contrasenia) {
        try {
            Authentication resultado = provider.authenticate(new UsernamePasswordAuthenticationToken(usuario, contrasenia));

            String mail = ((UsuarioAdDetails) resultado.getPrincipal()).getMail();
            if (mail == null || mail.isBlank()) {
                log.warn("[AD-AUTH] Login rechazado: la cuenta de AD no tiene el atributo mail cargado (usuario={})", usuario);
                return Optional.empty();
            }

            return Optional.of(mail.toLowerCase());
        } catch (CommunicationException e) {
            log.warn("[AD-AUTH] No se pudo conectar con el controlador de dominio: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo conectar con Active Directory. Probá de nuevo más tarde.");
        } catch (AuthenticationException e) {
            // Cuenta bloqueada, deshabilitada, vencida o contraseña incorrecta: la clase ya traduce (convertSubErrorCodesToExceptions) los subcódigos de error de AD a estas excepciones. Mismo tratamiento para todas — el mensaje al usuario final es siempre el mismo (ver AuthService).
            log.warn("[AD-AUTH] Autenticación rechazada por AD ({}): {}", e.getClass().getSimpleName(), usuario);
            return Optional.empty();
        }
    }

    private synchronized ActiveDirectoryLdapAuthenticationProvider obtenerProvider() {
        if (provider != null) {
            return provider;
        }
        if (!StringUtils.hasText(ldapUrl) || !StringUtils.hasText(domain) || !StringUtils.hasText(rootDn)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El login con Active Directory no está configurado.");
        }

        ActiveDirectoryLdapAuthenticationProvider nuevo = new ActiveDirectoryLdapAuthenticationProvider(domain, ldapUrl, rootDn);
        nuevo.setSearchFilter(SEARCH_FILTER);
        nuevo.setConvertSubErrorCodesToExceptions(true);
        nuevo.setUserDetailsContextMapper(new UsuarioAdContextMapper());
        nuevo.setContextEnvironmentProperties(Map.of("com.sun.jndi.ldap.connect.timeout", LDAP_TIMEOUT_MS, "com.sun.jndi.ldap.read.timeout", LDAP_TIMEOUT_MS));

        provider = nuevo;
        return provider;
    }

    // El mapper por defecto (LdapUserDetailsMapper) no expone atributos de la entrada LDAP; este solo lee el atributo mail de la cuenta ya autenticada y lo cuelga del UserDetails devuelto, para no depender de una búsqueda adicional contra el AD.
    private static class UsuarioAdContextMapper implements UserDetailsContextMapper {
        @Override
        public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
            return new UsuarioAdDetails(username, ctx.getStringAttribute("mail"));
        }

        @Override
        public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
            throw new UnsupportedOperationException("Solo lectura: SIENEP nunca escribe en el AD");
        }
    }

    // Package-private (no private): ActiveDirectoryVerifierServiceTest construye instancias directamente para simular la respuesta de un proveedor de AD.
    static class UsuarioAdDetails extends User {
        private final String mail;

        UsuarioAdDetails(String username, String mail) {
            super(username, "N/A", List.of());
            this.mail = mail;
        }

        String getMail() {
            return mail;
        }
    }
}
