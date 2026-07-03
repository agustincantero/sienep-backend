package com.fides.sienep.service;

import com.fides.sienep.repository.FuncionarioGcalRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// IMPORTANTE — alcance de este test class:
// GoogleOAuthService construye internamente instancias de GoogleAuthorizationCodeTokenRequest
// y GoogleRefreshTokenRequest y las ejecuta contra la red real de Google (no son
// dependencias inyectadas, se instancian con `new` adentro del método). Con Mockito
// puro no se puede interceptar eso sin refactorizar el service para inyectar un wrapper
// (ej. una interfaz GoogleTokenClient), cosa que no se hizo por alcance/tiempo.
//
// Por eso acá SOLO se prueba lo que corre antes de tocar la red:
// - generarUrlAutorizacion(): 100% local, arma una URL, no hay red involucrada.
// - procesarCallback(): las validaciones de state y de funcionario, que cortan ANTES
//   del intercambio con Google.
// El happy path de procesarCallback() (intercambio de code por tokens) y
// obtenerAccessToken() completo NO tienen test unitario — quedan para un test de
// integración con WireMock o un refactor a interfaz inyectable, si el equipo decide
// invertir el tiempo en eso.
@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private FuncionarioGcalRepository funcionarioGcalRepository;
    @Mock private OAuthStateStore oAuthStateStore;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleOAuthService, "clientId", "client-id-fake");
        ReflectionTestUtils.setField(googleOAuthService, "clientSecret", "client-secret-fake");
        ReflectionTestUtils.setField(googleOAuthService, "redirectUri", "https://sienep.app/api/auth/google/callback");
    }

    // ── generarUrlAutorizacion ────────────────────────────────

    @Test
    @DisplayName("Generar URL de autorización - Usa el state del store, no el idUsuario en texto plano")
    void generarUrlAutorizacion_UsaStateDelStore() {
        when(oAuthStateStore.generarState(2)).thenReturn("token-random-fake");

        String url = googleOAuthService.generarUrlAutorizacion(2);

        assertTrue(url.contains("state=token-random-fake"),
                "La URL debe llevar el token opaco del store, no el idUsuario");
        assertFalse(url.contains("state=2"),
                "La URL NO debe exponer el idUsuario en texto plano en el state (la vulnerabilidad original)");
        verify(oAuthStateStore).generarState(2);
    }

    @Test
    @DisplayName("Generar URL de autorización - Incluye access_type=offline y prompt=consent")
    void generarUrlAutorizacion_IncluyeParametrosParaRefreshToken() {
        when(oAuthStateStore.generarState(2)).thenReturn("token-random-fake");

        String url = googleOAuthService.generarUrlAutorizacion(2);

        // access_type=offline y prompt=consent son los dos parámetros que garantizan
        // que Google devuelva un refresh_token (sin ellos, RF22 completo se rompe).
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
        assertTrue(url.contains("client_id=client-id-fake"));
    }

    // ── procesarCallback: validaciones previas a la red ──────

    @Test
    @DisplayName("Procesar callback - State inválido o expirado (400), no llega a buscar funcionario")
    void procesarCallback_StateInvalido_Lanza400() {
        when(oAuthStateStore.consumir("state-invalido")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> googleOAuthService.procesarCallback("algun-code", "state-invalido"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(funcionarioRepository);
        verifyNoInteractions(funcionarioGcalRepository);
    }

    @Test
    @DisplayName("Procesar callback - State válido pero funcionario no existe (404)")
    void procesarCallback_FuncionarioNoEncontrado_Lanza404() {
        when(oAuthStateStore.consumir("state-valido")).thenReturn(Optional.of(99));
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> googleOAuthService.procesarCallback("algun-code", "state-valido"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(funcionarioGcalRepository);
    }

    @Test
    @DisplayName("Procesar callback - State ya usado (replay) se rechaza igual que uno inexistente")
    void procesarCallback_StateYaUsado_Lanza400() {
        // OAuthStateStore.consumir() borra el token al leerlo (single-use). Simulamos
        // el segundo intento devolviendo empty, que es justo lo que hace el store real
        // en un replay.
        when(oAuthStateStore.consumir("state-reusado")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> googleOAuthService.procesarCallback("algun-code", "state-reusado"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}