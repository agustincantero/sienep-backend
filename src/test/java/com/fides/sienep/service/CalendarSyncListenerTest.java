package com.fides.sienep.service;

import com.fides.sienep.event.AccionCalendar;
import com.fides.sienep.event.CalendarInstanciaEvent;
import com.fides.sienep.event.CalendarRecordatorioEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// CalendarSyncListener corre @Async @TransactionalEventListener(AFTER_COMMIT) en
// producción, pero acá se testea invocando los métodos directo (síncrono) — el
// framework de Spring es responsable de la asincronía/timing, no la lógica de
// negocio de este listener. Lo que sí es responsabilidad propia y se testea:
// (1) escribe de vuelta el eventId cuando GoogleCalendarService lo devuelve,
// (2) NO escribe nada si viene null (fallo real, o ELIMINAR), (3) un fallo en la
// escritura de vuelta se loguea [GCAL-DESINCRONIZADO] pero no propaga.
@ExtendWith(MockitoExtension.class)
class CalendarSyncListenerTest {

    @Mock private GoogleCalendarService googleCalendarService;
    @Mock private RecordatorioService recordatorioService;
    @Mock private InstanciaComunService instanciaComunService;

    @InjectMocks
    private CalendarSyncListener calendarSyncListener;

    @Test
    @DisplayName("onRecordatorioEvent - GoogleCalendarService devuelve un id, se persiste")
    void onRecordatorioEvent_ConEventId_Persiste() {
        CalendarRecordatorioEvent evento = CalendarRecordatorioEvent.builder()
                .idRecordatorio(1)
                .idNegRecordatorio("REC-2026-000001")
                .accion(AccionCalendar.CREAR)
                .build();
        when(googleCalendarService.procesar(evento)).thenReturn("evento-nuevo-id");

        calendarSyncListener.onRecordatorioEvent(evento);

        verify(recordatorioService).actualizarEventIdGoogle(1, "evento-nuevo-id");
    }

    @Test
    @DisplayName("onRecordatorioEvent - GoogleCalendarService devuelve null (fallo o ELIMINAR), no persiste")
    void onRecordatorioEvent_SinEventId_NoPersiste() {
        CalendarRecordatorioEvent evento = CalendarRecordatorioEvent.builder()
                .idRecordatorio(1)
                .idNegRecordatorio("REC-2026-000001")
                .accion(AccionCalendar.ELIMINAR)
                .build();
        when(googleCalendarService.procesar(evento)).thenReturn(null);

        calendarSyncListener.onRecordatorioEvent(evento);

        verify(recordatorioService, never()).actualizarEventIdGoogle(anyInt(), any());
    }

    @Test
    @DisplayName("onRecordatorioEvent - Falla la escritura de vuelta, no propaga")
    void onRecordatorioEvent_FallaEscrituraDeVuelta_NoPropaga() {
        CalendarRecordatorioEvent evento = CalendarRecordatorioEvent.builder()
                .idRecordatorio(1)
                .idNegRecordatorio("REC-2026-000001")
                .accion(AccionCalendar.CREAR)
                .build();
        when(googleCalendarService.procesar(evento)).thenReturn("evento-nuevo-id");
        doThrow(new RuntimeException("DB caída"))
                .when(recordatorioService).actualizarEventIdGoogle(1, "evento-nuevo-id");

        assertDoesNotThrow(() -> calendarSyncListener.onRecordatorioEvent(evento));
    }

    @Test
    @DisplayName("onInstanciaEvent - GoogleCalendarService devuelve un id, se persiste")
    void onInstanciaEvent_ConEventId_Persiste() {
        CalendarInstanciaEvent evento = CalendarInstanciaEvent.builder()
                .codInstancia(1)
                .idNegInstancia("INST-2026-000001")
                .accion(AccionCalendar.CREAR)
                .build();
        when(googleCalendarService.procesar(evento)).thenReturn("evento-nuevo-id");

        calendarSyncListener.onInstanciaEvent(evento);

        verify(instanciaComunService).actualizarEventIdGoogle(1, "evento-nuevo-id");
    }

    @Test
    @DisplayName("onInstanciaEvent - GoogleCalendarService devuelve null, no persiste")
    void onInstanciaEvent_SinEventId_NoPersiste() {
        CalendarInstanciaEvent evento = CalendarInstanciaEvent.builder()
                .codInstancia(1)
                .idNegInstancia("INST-2026-000001")
                .accion(AccionCalendar.ELIMINAR)
                .build();
        when(googleCalendarService.procesar(evento)).thenReturn(null);

        calendarSyncListener.onInstanciaEvent(evento);

        verify(instanciaComunService, never()).actualizarEventIdGoogle(anyInt(), any());
    }

    @Test
    @DisplayName("onInstanciaEvent - Falla la escritura de vuelta, no propaga")
    void onInstanciaEvent_FallaEscrituraDeVuelta_NoPropaga() {
        CalendarInstanciaEvent evento = CalendarInstanciaEvent.builder()
                .codInstancia(1)
                .idNegInstancia("INST-2026-000001")
                .accion(AccionCalendar.CREAR)
                .build();
        when(googleCalendarService.procesar(evento)).thenReturn("evento-nuevo-id");
        doThrow(new RuntimeException("DB caída"))
                .when(instanciaComunService).actualizarEventIdGoogle(1, "evento-nuevo-id");

        assertDoesNotThrow(() -> calendarSyncListener.onInstanciaEvent(evento));
    }
}
