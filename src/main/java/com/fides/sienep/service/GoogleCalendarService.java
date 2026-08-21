package com.fides.sienep.service;

import com.fides.sienep.event.AccionCalendar;
import com.fides.sienep.event.CalendarInstanciaEvent;
import com.fides.sienep.event.CalendarRecordatorioEvent;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Usa una Service Account con domain-wide delegation en vez del refresh_token
// individual de cada funcionario (modelo OAuth2 anterior, ya eliminado). Todos
// los eventos viven en un único calendario compartido (google.calendar.calendar-id),
// impersonando a google.calendar.impersonate-account — no depende de que ningún
// funcionario haya autorizado nada.
//
// impersonate-account tiene que ser una cuenta DENTRO del mismo Workspace que
// autorizó la delegación — no funciona con una cuenta de un dominio externo.
// Los asistentes invitados a cada evento (EventAttendee, más abajo) sí pueden
// ser de cualquier dominio, esa parte no tiene esa restricción.
@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    private static final String ZONA_HORARIA = "America/Montevideo";

    // Genérico a propósito, para ambos tipos de evento: el calendario ya no es
    // el personal de nadie, es compartido — el título/descripción libres del
    // recordatorio o la instancia podrían filtrar datos sensibles del alumno
    // (o del caso) a cualquiera con acceso a ese calendario o a la invitación
    // por mail. El detalle real se consulta en el sistema, no en Calendar.
    private static final String TITULO_GENERICO = "Cita SIENEP — ver detalle en el sistema";

    private static final Set<String> SCOPES = Set.of("https://www.googleapis.com/auth/calendar.events");

    @Value("${google.calendar.enabled}")
    private boolean enabled;

    @Value("${google.calendar.credentials:}")
    private String credentialsJson;

    @Value("${google.calendar.calendar-id}")
    private String calendarId;

    @Value("${google.calendar.impersonate-account}")
    private String impersonateAccount;

    @Value("${google.calendar.application-name}")
    private String applicationName;

    // Cliente construido una sola vez (lazy, en el primer uso real) y reutilizado:
    // a diferencia del refresh_token por funcionario de antes (vencía cada 1h y
    // obligaba a reconstruir todo en cada llamada), HttpCredentialsAdapter refresca
    // el token de la Service Account solo, por debajo, cuando hace falta.
    private Calendar calendarClient;

    public String procesar(CalendarRecordatorioEvent evento) {
        List<EventAttendee> attendees = new ArrayList<>();
        agregarAttendee(attendees, evento.getEmailFuncionario(), evento.getNombreFuncionario());
        String referencia = "recordatorio " + evento.getIdNegRecordatorio();
        return sincronizar(evento.getAccion(), evento.getOldEventId(), evento.getFechaHora(),
                attendees, evento.getRrule(), referencia);
    }

    public String procesar(CalendarInstanciaEvent evento) {
        List<EventAttendee> attendees = new ArrayList<>();
        agregarAttendee(attendees, evento.getEmailFuncionario(), evento.getNombreFuncionario());
        agregarAttendee(attendees, evento.getEmailAlumno(), evento.getNombreAlumno());
        String referencia = "instancia " + evento.getIdNegInstancia();
        return sincronizar(evento.getAccion(), evento.getOldEventId(), evento.getFechaHora(),
                attendees, null, referencia);
    }

    // ── Núcleo común a ambos tipos de evento ────────────────────────────────

    private String sincronizar(AccionCalendar accion, String oldEventId, LocalDateTime fechaHora,
                                List<EventAttendee> attendees, String rrule, String referenciaLog) {
        if (!enabled) {
            if (accion == AccionCalendar.ELIMINAR) {
                log.info("[GCAL] Modo simulado (enabled=false): se omite eliminar el evento {} de {}",
                        oldEventId, referenciaLog);
                return null;
            }
            String idSimulado = "SIMULADO-" + System.currentTimeMillis();
            log.info("[GCAL] Modo simulado (enabled=false): se omite {} el evento real de {}, devolviendo id simulado {}",
                    accion, referenciaLog, idSimulado);
            return idSimulado;
        }

        try {
            Calendar service = obtenerCliente();

            if (accion == AccionCalendar.ELIMINAR) {
                service.events().delete(calendarId, oldEventId).setSendUpdates("all").execute();
                log.info("[GCAL] Evento eliminado: {} ({})", oldEventId, referenciaLog);
                return null;
            }

            Event evento = construirEvento(fechaHora, attendees, rrule);

            if (accion == AccionCalendar.ACTUALIZAR && oldEventId != null) {
                service.events().update(calendarId, oldEventId, evento).setSendUpdates("all").execute();
                log.info("[GCAL] Evento actualizado: {} ({})", oldEventId, referenciaLog);
                return oldEventId;
            }

            Event eventoCreado = service.events().insert(calendarId, evento).setSendUpdates("all").execute();
            log.info("[GCAL] Evento creado: {} ({})", eventoCreado.getId(), referenciaLog);
            return eventoCreado.getId();

        } catch (Exception e) {
            log.warn("[GCAL] No se pudo sincronizar con Google Calendar ({}, {}): {}",
                    accion, referenciaLog, e.getMessage());
            return null;
        }
    }

    private void agregarAttendee(List<EventAttendee> attendees, String email, String nombre) {
        if (email == null || email.isBlank()) {
            return;
        }
        attendees.add(new EventAttendee().setEmail(email).setDisplayName(nombre));
    }

    // Package-private (no private): testeable directo desde GoogleCalendarServiceTest
    // sin reflection — es la parte de armar el Event que no depende del SDK/red.
    Event construirEvento(LocalDateTime fechaHora, List<EventAttendee> attendees, String rrule) {
        Event evento = new Event();
        evento.setSummary(TITULO_GENERICO);
        evento.setAttendees(attendees);

        ZonedDateTime inicio = fechaHora.atZone(ZoneId.of(ZONA_HORARIA));
        ZonedDateTime fin = inicio.plusHours(1); // duración por defecto: 1 hora

        evento.setStart(new EventDateTime()
                .setDateTime(toGoogleDateTime(inicio))
                .setTimeZone(ZONA_HORARIA));
        evento.setEnd(new EventDateTime()
                .setDateTime(toGoogleDateTime(fin))
                .setTimeZone(ZONA_HORARIA));

        if (rrule != null) {
            evento.setRecurrence(List.of(rrule));
        }

        evento.setReminders(new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(List.of(
                        new EventReminder().setMethod("email").setMinutes(24 * 60),
                        new EventReminder().setMethod("popup").setMinutes(30)
                )));

        return evento;
    }

    private synchronized Calendar obtenerCliente() throws Exception {
        if (calendarClient != null) {
            return calendarClient;
        }

        // createScoped() devuelve GoogleCredentials (no ServiceAccountCredentials) en esta
        // versión de la librería — por eso createDelegated() (específico de
        // ServiceAccountCredentials, para domain-wide delegation) va primero.
        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                .createDelegated(impersonateAccount)
                .createScoped(SCOPES);

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        calendarClient = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName(applicationName).build();

        return calendarClient;
    }

    private DateTime toGoogleDateTime(ZonedDateTime zdt) {
        return new DateTime(zdt.toInstant().toEpochMilli());
    }
}
