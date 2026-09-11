package com.fides.sienep.security;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.utils.JwtTokenUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// P0 de la auditoría (sección 5): antes de este test, JwtRequestFilter tenía 0%
// de cobertura pese a ser el único punto donde se valida un JWT en cada request
// autenticado — cualquier regresión acá (ej. no rechazar un token vencido o con
// firma inválida) pasaba totalmente desapercibida para la suite.
@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    private static final String SECRET = "test-secret-key-only-for-context-load-sienep";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Mock private CustomUsuarioDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private JwtTokenUtil jwtTokenUtil;
    private JwtRequestFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(SECRET);
        filter = new JwtRequestFilter(userDetailsService, jwtTokenUtil);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String tokenValido(String idUsuario) {
        return jwtTokenUtil.generateToken(Integer.parseInt(idUsuario), "ADMINISTRADOR");
    }

    private String tokenVencido(String idUsuario) {
        return Jwts.builder()
                .subject(idUsuario)
                .issuedAt(new Date(System.currentTimeMillis() - 10_000_000))
                .expiration(new Date(System.currentTimeMillis() - 1_000_000))
                .signWith(SECRET_KEY)
                .compact();
    }

    private SienepUserDetails userDetails(String idUsuario, LocalDateTime credencialesVigentesDesde) {
        return userDetails(idUsuario, credencialesVigentesDesde, EstadoLogico.ACTIVO);
    }

    private SienepUserDetails userDetails(String idUsuario, LocalDateTime credencialesVigentesDesde,
                                           EstadoLogico estado) {
        return new SienepUserDetails(idUsuario, "hash-irrelevante",
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")), credencialesVigentesDesde, estado);
    }

    @Test
    @DisplayName("Sin header Authorization - deja pasar la request sin autenticar")
    void sinHeaderAuthorization_DejaPasarSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        verify(response, never()).sendError(anyInt(), any());
    }

    @Test
    @DisplayName("Header Authorization sin prefijo Bearer - deja pasar sin autenticar")
    void headerSinPrefijoBearer_DejaPasarSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Token abc123");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Token malformado - responde 401 y no continúa la cadena")
    void tokenMalformado_Responde401_NoContinuaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer esto-no-es-un-jwt-valido");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token inválido o vencido"));
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Token con firma manipulada - responde 401 y no continúa la cadena")
    void tokenConFirmaManipulada_Responde401_NoContinuaCadena() throws Exception {
        String valido = tokenValido("2");
        String tamperado = valido.substring(0, valido.length() - 4) + "abcd";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tamperado);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token inválido o vencido"));
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Token vencido - responde 401 y no continúa la cadena")
    void tokenVencido_Responde401_NoContinuaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenVencido("2"));

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token inválido o vencido"));
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Token válido pero usuario no encontrado - responde 401 y no continúa la cadena")
    void tokenValidoUsuarioNoEncontrado_Responde401_NoContinuaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenValido("99"));
        when(userDetailsService.loadUserByUsername("99"))
                .thenThrow(new UsernameNotFoundException("no existe"));

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token inválido"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token emitido antes de un cambio de contraseña - responde 401 (credenciales no vigentes)")
    void tokenEmitidoAntesDeCambioDeContrasenia_Responde401() throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        // credencialesVigentesDesde en el futuro respecto al issuedAt del token —
        // simula que la contraseña se cambió después de emitido este token.
        when(userDetailsService.loadUserByUsername("2"))
                .thenReturn(userDetails("2", LocalDateTime.now().plusDays(1)));

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token inválido o vencido"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token válido - autentica en el SecurityContext y continúa la cadena")
    void tokenValido_AutenticaYContinuaCadena() throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userDetailsService.loadUserByUsername("2"))
                .thenReturn(userDetails("2", null));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNullAndAuthenticated(auth);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), any());
    }

    @Test
    @DisplayName("Ya hay una autenticación en el contexto - no vuelve a consultar al userDetailsService")
    void yaAutenticado_NoVuelveAConsultarUserDetailsService() throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("2", null, List.of()));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    // ── PENDIENTE_DE_ACTIVACION: whitelist de endpoints ─────────────────────────

    @Test
    @DisplayName("Usuario PENDIENTE_DE_ACTIVACION - GET /auth/me continúa la cadena")
    void pendienteActivacion_AuthMe_ContinuaLaCadena() throws Exception {
        assertRutaPermitidaPendienteActivacion("GET", "/auth/me");
    }

    @Test
    @DisplayName("Usuario PENDIENTE_DE_ACTIVACION - POST /auth/logout continúa la cadena")
    void pendienteActivacion_AuthLogout_ContinuaLaCadena() throws Exception {
        assertRutaPermitidaPendienteActivacion("POST", "/auth/logout");
    }

    @Test
    @DisplayName("Usuario PENDIENTE_DE_ACTIVACION - PATCH /funcionarios/{id}/contrasenia continúa la cadena")
    void pendienteActivacion_CambiarContraseniaFuncionario_ContinuaLaCadena() throws Exception {
        assertRutaPermitidaPendienteActivacion("PATCH", "/funcionarios/2/contrasenia");
    }

    @Test
    @DisplayName("Usuario PENDIENTE_DE_ACTIVACION - PATCH /estudiantes/{id}/contrasenia continúa la cadena")
    void pendienteActivacion_CambiarContraseniaEstudiante_ContinuaLaCadena() throws Exception {
        assertRutaPermitidaPendienteActivacion("PATCH", "/estudiantes/2/contrasenia");
    }

    @Test
    @DisplayName("Usuario PENDIENTE_DE_ACTIVACION - ruta fuera de la whitelist responde 403 y no continúa la cadena")
    void pendienteActivacion_RutaNoWhitelisteada_Responde403_NoContinuaCadena() throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/funcionarios");
        when(userDetailsService.loadUserByUsername("2"))
                .thenReturn(userDetails("2", null, EstadoLogico.PENDIENTE_DE_ACTIVACION));

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN),
                eq("Debe cambiar su contraseña temporal antes de continuar."));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Usuario PENDIENTE_DE_ACTIVACION - POST /funcionarios/{id}/contrasenia (reset admin, mismo path " +
            "distinto método) responde 403")
    void pendienteActivacion_ResetAdminMismoPathDistintoMetodo_Responde403() throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/funcionarios/2/contrasenia");
        when(userDetailsService.loadUserByUsername("2"))
                .thenReturn(userDetails("2", null, EstadoLogico.PENDIENTE_DE_ACTIVACION));

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Usuario ACTIVO - ruta cualquiera no se ve afectada por el enforcement de PENDIENTE_DE_ACTIVACION")
    void usuarioActivo_RutaNoWhitelisteada_ContinuaLaCadena() throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userDetailsService.loadUserByUsername("2"))
                .thenReturn(userDetails("2", null, EstadoLogico.ACTIVO));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), any());
    }

    private void assertRutaPermitidaPendienteActivacion(String metodo, String path) throws Exception {
        String token = tokenValido("2");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getMethod()).thenReturn(metodo);
        when(request.getRequestURI()).thenReturn(path);
        when(userDetailsService.loadUserByUsername("2"))
                .thenReturn(userDetails("2", null, EstadoLogico.PENDIENTE_DE_ACTIVACION));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), any());
    }

    private void assertNotNullAndAuthenticated(Authentication auth) {
        org.junit.jupiter.api.Assertions.assertNotNull(auth);
        org.junit.jupiter.api.Assertions.assertEquals("2", ((SienepUserDetails) auth.getPrincipal()).getUsername());
        org.junit.jupiter.api.Assertions.assertTrue(
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")));
    }
}
