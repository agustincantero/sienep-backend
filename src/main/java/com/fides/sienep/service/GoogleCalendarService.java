package com.fides.sienep.service;

import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.utils.RecurrenceRuleBuilder;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String APP_NAME      = "SIENEP";
    private static final String CALENDAR_ID   = "primary"; // calendario personal del usuario
    private static final String ZONA_HORARIA  = "America/Montevideo";

    private final GoogleOAuthService googleOAuthService;
    private final RecurrenceRuleBuilder recurrenceRuleBuilder;

    // ── Crear evento ──────────────────────────────────────────────────────
    // Retorna el eventId de Google, que se guarda en Recordatorio.eventIdGoogle.
    // Si falla, retorna null — el caller decide si loguear warn o ignorar.

    public String crearEvento(Recordatorio recordatorio, String refreshToken) {
        try {
            Calendar service = construirServicio(refreshToken);
            Event evento = construirEvento(recordatorio);

            Event eventoCreado = service.events()
                    .insert(CALENDAR_ID, evento)
                    .execute();

            log.info("[GCAL] Evento creado: {} para recordatorio {}",
                    eventoCreado.getId(), recordatorio.getIdNegRecordatorio());
            return eventoCreado.getId();

        } catch (Exception e) {
            log.warn("[GCAL] No se pudo crear el evento para recordatorio {}: {}",
                    recordatorio.getIdNegRecordatorio(), e.getMessage());
            return null;
        }
    }

    // ── Actualizar evento ─────────────────────────────────────────────────
    // Se llama desde RecordatorioService.actualizar() cuando el recordatorio ya
    // tenía un evento sincronizado y el responsable no cambió (RF22).

    public void actualizarEvento(Recordatorio recordatorio, String eventIdGoogle, String refreshToken) {
        try {
            Calendar service = construirServicio(refreshToken);
            Event evento = construirEvento(recordatorio);

            service.events().update(CALENDAR_ID, eventIdGoogle, evento).execute();
            log.info("[GCAL] Evento actualizado: {} para recordatorio {}",
                    eventIdGoogle, recordatorio.getIdNegRecordatorio());

        } catch (Exception e) {
            log.warn("[GCAL] No se pudo actualizar el evento {} para recordatorio {}: {}",
                    eventIdGoogle, recordatorio.getIdNegRecordatorio(), e.getMessage());
        }
    }

    // ── Eliminar evento ───────────────────────────────────────────────────
    // Se llama desde RecordatorioService.desactivar() si eventIdGoogle != null.

    public void eliminarEvento(String eventIdGoogle, String refreshToken) {
        try {
            Calendar service = construirServicio(refreshToken);
            service.events().delete(CALENDAR_ID, eventIdGoogle).execute();
            log.info("[GCAL] Evento eliminado: {}", eventIdGoogle);
        } catch (Exception e) {
            log.warn("[GCAL] No se pudo eliminar el evento {}: {}", eventIdGoogle, e.getMessage());
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private Event construirEvento(Recordatorio recordatorio) {
        Event evento = new Event();
        evento.setSummary("[SIENEP] " + recordatorio.getTitulo());
        evento.setDescription(
                "Recordatorio SIENEP\n" +
                        "ID Recordatorio: " + recordatorio.getIdNegRecordatorio() + "\n" +
                        "Instancia: " + recordatorio.getInstancia().getIdNegInstancia() + "\n" +
                        "Tipo: " + recordatorio.getTipo().name()
        );

        // Fecha/hora de inicio — LocalDateTime a ZonedDateTime con zona de Uruguay
        ZonedDateTime inicio = recordatorio.getFechaHora()
                .atZone(ZoneId.of(ZONA_HORARIA));
        ZonedDateTime fin    = inicio.plusHours(1); // duración por defecto: 1 hora

        evento.setStart(new EventDateTime()
                .setDateTime(toGoogleDateTime(inicio))
                .setTimeZone(ZONA_HORARIA));
        evento.setEnd(new EventDateTime()
                .setDateTime(toGoogleDateTime(fin))
                .setTimeZone(ZONA_HORARIA));

        // Recurrencia — solo si el recordatorio tiene frecuencia asignada
        if (recordatorio.getFrecuencia() != null) {
            String rrule = recurrenceRuleBuilder.construir(recordatorio);
            if (rrule != null) {
                evento.setRecurrence(List.of(rrule));
            }
        }

        // Recordatorio por email 24h antes y popup 30 min antes
        evento.setReminders(new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(List.of(
                        new EventReminder().setMethod("email").setMinutes(24 * 60),
                        new EventReminder().setMethod("popup").setMinutes(30)
                )));

        return evento;
    }

    private Calendar construirServicio(String refreshToken) throws GeneralSecurityException, IOException {
        String accessToken = googleOAuthService.obtenerAccessToken(refreshToken);

        // Credencial efímera construida desde el access_token obtenido con el refresh_token
        OAuth2Credentials credentials = OAuth2Credentials.create(
                new AccessToken(accessToken, new Date(System.currentTimeMillis() + 3600_000L))
        );
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName(APP_NAME).build();
    }

    private DateTime toGoogleDateTime(ZonedDateTime zdt) {
        return new DateTime(zdt.toInstant().toEpochMilli());
    }
}