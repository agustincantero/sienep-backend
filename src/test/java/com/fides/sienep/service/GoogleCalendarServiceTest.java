package com.fides.sienep.service;

import com.fides.sienep.model.Instancia;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.model.TipoRecordatorio;
import com.fides.sienep.utils.RecurrenceRuleBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// IMPORTANTE — alcance de este test class:
// GoogleCalendarService.construirServicio() arma un cliente real del SDK de Google
// (Calendar.Builder) y crearEvento()/eliminarEvento() terminan en un .execute() que
// hace una llamada HTTP real a googleapis.com. Nada de eso está inyectado como
// dependencia mockeable — es imposible de interceptar con Mockito puro sin refactorizar
// a una interfaz wrapper (o sumar WireMock, que el proyecto no usa en ningún lado).
//
// Lo único genuinamente testeable sin tocar la red es la ruta de fallo ANTES de llegar
// al SDK: cuando GoogleOAuthService.obtenerAccessToken() falla (token vencido, refresh
// token revocado, etc.), crearEvento()/eliminarEvento() deben devolver limpio (null o
// void) y loguear warn, nunca propagar la excepción — es el comportamiento non-blocking
// que sostiene todo RF22. El happy path (evento creado con éxito) no tiene test
// unitario acá; para cubrirlo de verdad haría falta un test de integración con
// WireMock simulando la API de Calendar.
@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {

    @Mock private GoogleOAuthService googleOAuthService;
    @Mock private RecurrenceRuleBuilder recurrenceRuleBuilder;

    @InjectMocks
    private GoogleCalendarService googleCalendarService;

    private Recordatorio recordatorio;

    @BeforeEach
    void setUp() {
        Instancia instancia = mock(Instancia.class);
        lenient().when(instancia.getIdNegInstancia()).thenReturn("INST-2026-000001");

        recordatorio = new Recordatorio();
        recordatorio.setCodRecordatorio(1);
        recordatorio.setIdNegRecordatorio("REC-2026-000001");
        recordatorio.setTitulo("Llamar a Ana");
        recordatorio.setTipo(TipoRecordatorio.LLAMADA);
        recordatorio.setFechaHora(LocalDateTime.now().plusDays(1));
        recordatorio.setInstancia(instancia);
    }

    @Test
    @DisplayName("Crear evento - Falla al obtener access_token, devuelve null en vez de lanzar")
    void crearEvento_FallaAlObtenerAccessToken_DevuelveNull() {
        when(googleOAuthService.obtenerAccessToken("refresh-token-invalido"))
                .thenThrow(new RuntimeException("Token revocado por el usuario"));

        String resultado = assertDoesNotThrow(
                () -> googleCalendarService.crearEvento(recordatorio, "refresh-token-invalido"));

        assertNull(resultado);
    }

    @Test
    @DisplayName("Eliminar evento - Falla al obtener access_token, no lanza excepción")
    void eliminarEvento_FallaAlObtenerAccessToken_NoLanza() {
        when(googleOAuthService.obtenerAccessToken("refresh-token-invalido"))
                .thenThrow(new RuntimeException("Token revocado por el usuario"));

        assertDoesNotThrow(
                () -> googleCalendarService.eliminarEvento("evento-fake-id", "refresh-token-invalido"));
    }
}