package com.fides.sienep.service;

import com.fides.sienep.event.CalendarInstanciaEvent;
import com.fides.sienep.event.CalendarRecordatorioEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Consume los eventos que publican RecordatorioService/InstanciaComunService.
// AFTER_COMMIT: la sync con Google Calendar corre recién después de que la
// transacción de negocio ya confirmó — un problema de red con Google nunca
// puede interferir con esa transacción, ni sentirse en la latencia del request
// (@Async corre en otro thread). GoogleCalendarService nunca propaga excepciones
// (loguea y devuelve null), así que estos métodos no necesitan try/catch propio
// para la llamada a Calendar — sí para la escritura de vuelta del eventId,
// porque esa persistencia puede fallar independientemente (mismo patrón
// [GCAL-DESINCRONIZADO] que ya usaban los services antes de esta migración).
@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarSyncListener {

    private final GoogleCalendarService googleCalendarService;
    private final RecordatorioService recordatorioService;
    private final InstanciaComunService instanciaComunService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecordatorioEvent(CalendarRecordatorioEvent evento) {
        String eventId = googleCalendarService.procesar(evento);
        if (eventId == null) {
            return;
        }
        try {
            recordatorioService.actualizarEventIdGoogle(evento.getIdRecordatorio(), eventId);
        } catch (Exception e) {
            log.error("[GCAL-DESINCRONIZADO] Se sincronizó el evento {} para el recordatorio {}, "
                            + "pero falló su persistencia local: {}",
                    eventId, evento.getIdNegRecordatorio(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstanciaEvent(CalendarInstanciaEvent evento) {
        String eventId = googleCalendarService.procesar(evento);
        if (eventId == null) {
            return;
        }
        try {
            instanciaComunService.actualizarEventIdGoogle(evento.getCodInstancia(), eventId);
        } catch (Exception e) {
            log.error("[GCAL-DESINCRONIZADO] Se sincronizó el evento {} para la instancia {}, "
                            + "pero falló su persistencia local: {}",
                    eventId, evento.getIdNegInstancia(), e.getMessage());
        }
    }
}
