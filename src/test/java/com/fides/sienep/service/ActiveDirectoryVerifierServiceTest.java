package com.fides.sienep.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// Ejerce la lógica de ActiveDirectoryVerifierService con un ActiveDirectoryLdapAuthenticationProvider simulado (autenticar(), package-private): no requiere un AD real, a diferencia de un test contra el controlador de dominio.
@ExtendWith(MockitoExtension.class)
class ActiveDirectoryVerifierServiceTest {

    @Mock private ActiveDirectoryLdapAuthenticationProvider provider;
    @Mock private Authentication authenticationResultado;

    private final ActiveDirectoryVerifierService service = new ActiveDirectoryVerifierService();

    @Test
    @DisplayName("Credenciales válidas y cuenta de AD con mail cargado - devuelve el email en minúsculas")
    void autenticar_CredencialesValidas_DevuelveEmail() {
        when(provider.authenticate(org.mockito.ArgumentMatchers.any())).thenReturn(authenticationResultado);
        when(authenticationResultado.getPrincipal())
                .thenReturn(new ActiveDirectoryVerifierService.UsuarioAdDetails("jgarcia", "Juan.Garcia@UTEC.edu.uy"));

        Optional<String> resultado = service.autenticar(provider, "jgarcia", "Corp123!");

        assertTrue(resultado.isPresent());
        assertEquals("juan.garcia@utec.edu.uy", resultado.get());
    }

    @Test
    @DisplayName("Cuenta de AD sin el atributo mail cargado - no hay forma de enlazarla, devuelve vacío")
    void autenticar_SinAtributoMail_DevuelveVacio() {
        when(provider.authenticate(org.mockito.ArgumentMatchers.any())).thenReturn(authenticationResultado);
        when(authenticationResultado.getPrincipal())
                .thenReturn(new ActiveDirectoryVerifierService.UsuarioAdDetails("jgarcia", null));

        Optional<String> resultado = service.autenticar(provider, "jgarcia", "Corp123!");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("AD rechaza las credenciales (usuario o contraseña incorrectos) - devuelve vacío")
    void autenticar_CredencialesRechazadas_DevuelveVacio() {
        when(provider.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        Optional<String> resultado = service.autenticar(provider, "jgarcia", "incorrecta");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Cuenta de AD bloqueada - devuelve vacío igual que una contraseña incorrecta")
    void autenticar_CuentaBloqueada_DevuelveVacio() {
        when(provider.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new LockedException("Cuenta bloqueada"));

        Optional<String> resultado = service.autenticar(provider, "jgarcia", "Corp123!");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Controlador de dominio inalcanzable - responde 503 en lugar de un 401 engañoso")
    void autenticar_ControladorDeDominioInalcanzable_Lanza503() {
        when(provider.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new org.springframework.ldap.CommunicationException(
                        new javax.naming.CommunicationException("timeout")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.autenticar(provider, "jgarcia", "Corp123!"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("AD no configurado (variables de entorno ausentes) - responde 503 sin intentar conectarse")
    void emailVerificado_AdNoConfigurado_Lanza503() {
        ActiveDirectoryVerifierService sinConfigurar = new ActiveDirectoryVerifierService();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sinConfigurar.emailVerificado("jgarcia", "Corp123!"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
