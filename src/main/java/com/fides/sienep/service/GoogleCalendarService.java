package com.fides.sienep.service;

import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.model.TipoFrecuencia;
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

    // ── Crear evento ──────────────────────────────────────────────────────
    // Retorna el eventId de Google, que se guarda en Recordatorio.eventIdGoogle.
    // Si falla, retorna null — el caller decide si loguear warn o ignorar.

    public String crearEvento(Recordatorio recordatorio, String refreshToken) {
        try {
            Calendar service = construirServicio(refreshToken);

            Event evento = new Event();
            evento.setSummary("[SIENEP] " + recordatorio.getTitulo());
            evento.setDescription(
                    "Recordatorio SIENEP\n" +
                            "ID: " + recordatorio.getIdNegRecordatorio() + "\n" +
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
                String rrule = construirRrule(recordatorio);
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

    // ── Eliminar evento ───────────────────────────────────────────────────
    // Se llama desde RecordatorioService.darDeBaja() si eventIdGoogle != null.

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

    // ── Construcción de RRULE desde modelo Frecuencia ────────────────────
    //
    // Mapeo:
    //   DIARIA        → FREQ=DAILY
    //   SEMANAL       → FREQ=WEEKLY;BYDAY=<DiaSemana>
    //   MENSUAL       → FREQ=MONTHLY
    //   PERSONALIZADA → FREQ=DAILY;INTERVAL=<intDias>
    //
    // Si el recordatorio tiene fechaFin → UNTIL=<fecha>
    // Si tiene nroOcurrencias           → COUNT=<n>
    // Si no tiene ninguno de los dos    → sin límite (poco probable por validaciones del DTO)

    private String construirRrule(Recordatorio recordatorio) {
        Frecuencia frecuencia = recordatorio.getFrecuencia();
        if (frecuencia == null) {
            return null;
        }

        StringBuilder rrule = new StringBuilder("RRULE:FREQ=");

        if (frecuencia.getTipo() == TipoFrecuencia.DIARIA) {
            rrule.append("DAILY");

        } else if (frecuencia.getTipo() == TipoFrecuencia.SEMANAL) {
            rrule.append("WEEKLY");
            if (frecuencia.getDiaSemana() != null) {
                // DiaSemana enum → abreviatura RRULE (ej. LUNES → MO)
                String byday = mapearDiaSemana(frecuencia.getDiaSemana().name());
                if (byday != null) {
                    rrule.append(";BYDAY=").append(byday);
                }
            }

        } else if (frecuencia.getTipo() == TipoFrecuencia.MENSUAL) {
            rrule.append("MONTHLY");

        } else if (frecuencia.getTipo() == TipoFrecuencia.PERSONALIZADA) {
            rrule.append("DAILY");
            if (frecuencia.getIntDias() != null && frecuencia.getIntDias() > 0) {
                rrule.append(";INTERVAL=").append(frecuencia.getIntDias());
            }
        } else {
            return null;
        }

        // Límite: fechaFin tiene precedencia sobre nroOcurrencias
        if (recordatorio.getFechaFin() != null) {
            // UNTIL requiere formato YYYYMMDD en RRULE
            String hasta = recordatorio.getFechaFin()
                    .toString()
                    .replace("-", "");
            rrule.append(";UNTIL=").append(hasta);
        } else if (recordatorio.getNroOcurrencias() != null && recordatorio.getNroOcurrencias() > 0) {
            rrule.append(";COUNT=").append(recordatorio.getNroOcurrencias());
        }

        return rrule.toString();
    }

    private String mapearDiaSemana(String diaSemana) {
        switch (diaSemana) {
            case "LUNES":     return "MO";
            case "MARTES":    return "TU";
            case "MIERCOLES": return "WE";
            case "JUEVES":    return "TH";
            case "VIERNES":   return "FR";
            case "SABADO":    return "SA";
            case "DOMINGO":   return "SU";
            default:          return null;
        }
    }
}