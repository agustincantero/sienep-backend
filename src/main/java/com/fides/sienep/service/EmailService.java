package com.fides.sienep.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String REMITENTE_EMAIL = "notificaciones@sienep.app";
    private static final String REMITENTE_NOMBRE = "Notificaciones SIENEP";
    // Mismo patrón que ReportePdfService, para que la fecha se vea igual en todo el
    // sistema — sin esto sale el ISO crudo de LocalDateTime.toString() (ej. "2026-08-25T15:00").
    private static final DateTimeFormatter FORMATTER_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Plantillas HTML en src/main/resources/email-templates/ (ver renderizarPlantilla).
    private static final String PLANTILLA_CONTRASENIA_TEMPORAL = "email-templates/contrasenia-temporal.html";
    private static final String PLANTILLA_CONTRASENIA_RESTABLECIDA = "email-templates/contrasenia-restablecida.html";
    private static final String PLANTILLA_RESTABLECIMIENTO_LINK = "email-templates/restablecimiento-link.html";
    private static final String PLANTILLA_NOTIFICACION_INSTANCIA = "email-templates/notificacion-instancia.html";
    private static final String PLANTILLA_NOTIFICACION_RECORDATORIO = "email-templates/notificacion-recordatorio.html";
    private static final String PLANTILLA_RECORDATORIO_PROXIMO = "email-templates/recordatorio-proximo.html";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final RestClient brevoRestClient;

    // Las plantillas son recursos estáticos del jar: se leen una sola vez, no en cada envío.
    private final Map<String, String> plantillasCargadas = new ConcurrentHashMap<>();

    // Campo separado (no final): mismo motivo que AuthService.frontendUrl — Lombok no
    // copia @Value a los parámetros del constructor generado por @RequiredArgsConstructor.
    @Value("${app.frontend.url}")
    private String frontendUrl;

    // API HTTP de Brevo en vez de SMTP: Render (free tier) bloquea tráfico
    // saliente a los puertos SMTP (25/465/587) desde el 26/09/2025, así que
    // JavaMailSender nunca podía conectar. La API HTTP va por 443, mismo
    // puerto que ya usa GoogleCalendarService contra la API de Google.
    //
    // Con html != null, Brevo manda un mensaje multipart/alternative (texto + HTML):
    // el textContent queda de fallback para clientes de correo que no renderizan HTML
    // o marcan como spam un mensaje sin alternativa de texto plano.
    private void enviarEmailBrevo(String destinatario, String asunto, String texto, String html) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "sender", Map.of("name", REMITENTE_NOMBRE, "email", REMITENTE_EMAIL),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "textContent", texto
        ));
        if (html != null) {
            body.put("htmlContent", html);
        }
        brevoRestClient.post()
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // Reemplaza los placeholders {{CLAVE}} de una plantilla HTML. Los valores se escapan con
    // HtmlUtils antes de insertarlos (nombre/contraseña/títulos vienen de datos de usuario)
    // para que un valor con caracteres tipo "<" no rompa el markup del mail.
    //
    // Un solo pase con regex: lo que se inserta nunca se vuelve a escanear. Reemplazar clave
    // por clave iterando el Map dejaba que un valor con "{{OTRA_CLAVE}}" (ej. un título escrito
    // por un usuario) fuera pisado por un reemplazo posterior, según el orden de iteración.
    //
    // Un placeholder sin valor corta el envío (IllegalStateException): dejarlo literal mandaba
    // un mail con "{{CLAVE}}" a la vista del destinatario — típico de agregar un placeholder a
    // la plantilla y olvidarse de pasarlo desde el método que la usa. Quien llama ya captura
    // cualquier Exception, la registra y responde el error de envío, así que el fallo queda
    // visible en el log en vez de salir un correo roto. (Package-private para poder testearlo.)
    String renderizarPlantilla(String recursoClasspath, Map<String, String> valores) {
        String plantilla = plantillasCargadas.computeIfAbsent(recursoClasspath, this::cargarPlantilla);
        return PLACEHOLDER.matcher(plantilla).replaceAll(coincidencia -> {
            String valor = valores.get(coincidencia.group(1));
            if (valor == null) {
                throw new IllegalStateException("La plantilla " + recursoClasspath
                        + " usa el placeholder {{" + coincidencia.group(1) + "}} sin valor asignado");
            }
            return Matcher.quoteReplacement(HtmlUtils.htmlEscape(valor));
        });
    }

    private String cargarPlantilla(String recursoClasspath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(recursoClasspath)) {
            if (is == null) {
                throw new IllegalStateException("Plantilla de email no encontrada: " + recursoClasspath);
            }
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la plantilla de email " + recursoClasspath, e);
        }
    }

    public void enviarContraseniaTemporal(String destinatario, String nombreUsuario, String contrasenia) {
        try {
            // El recordatorio no es solo buena práctica: mientras la cuenta esté
            // PENDIENTE_DE_ACTIVACION, JwtRequestFilter bloquea con 403 cualquier
            // endpoint que no sea cambiar la contraseña.
            String texto = "¡Hola, " + nombreUsuario + "!\n\n" +
                    "Tu cuenta fue creada con éxito.\n" +
                    "Tu contraseña temporal es: " + contrasenia + "\n\n" +
                    "Por seguridad, debes cambiar tu contraseña en tu primer ingreso.";
            String html = renderizarPlantilla(PLANTILLA_CONTRASENIA_TEMPORAL, Map.of(
                    "NOMBRE_USUARIO", nombreUsuario,
                    "CONTRASENIA_TEMPORAL", contrasenia,
                    "URL_INICIO_SESION", frontendUrl
            ));
            enviarEmailBrevo(destinatario, "Acceso al sistema - Contraseña temporal", texto, html);
            log.info("[AUDIT] Email de contraseña temporal enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("[AUDIT] Error al enviar email a {}: {}", destinatario, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el email a " + destinatario);
        }
    }

    public void enviarContraseniaRestablecida(String destinatario, String nombreUsuario, String contrasenia) {
        try {
            // Mismo motivo que en enviarContraseniaTemporal: el reset admin también deja
            // la cuenta PENDIENTE_DE_ACTIVACION, bloqueada por JwtRequestFilter hasta el cambio.
            String texto = "¡Hola, " + nombreUsuario + "!\n\n" +
                    "Se generó una nueva contraseña para tu cuenta.\n" +
                    "Tu contraseña temporal es: " + contrasenia + "\n\n" +
                    "Por seguridad, debes cambiar tu contraseña en tu próximo ingreso.";
            String html = renderizarPlantilla(PLANTILLA_CONTRASENIA_RESTABLECIDA, Map.of(
                    "NOMBRE_USUARIO", nombreUsuario,
                    "CONTRASENIA_TEMPORAL", contrasenia,
                    "URL_INICIO_SESION", frontendUrl
            ));
            enviarEmailBrevo(destinatario, "Restablecimiento de contraseña - SIENEP", texto, html);
            log.info("[AUDIT] Email de restablecimiento enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("[AUDIT] Error al enviar email a {}: {}", destinatario, e.getMessage());
            // 503 (no 500): el fallo es del servicio externo, no un bug del servidor —
            // restablecerContrasenia() no captura esta excepción a propósito (a
            // diferencia de crear(), acá sí hay una contraseña previa que preservar
            // vía rollback si el envío falla).
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo enviar el email a " + destinatario);
        }
    }

    // Flujo "olvidé mi contraseña": a diferencia de enviarContraseniaRestablecida
    // (usada cuando un admin autenticado resetea la de otro), acá no se manda ninguna
    // contraseña — se manda un link con un token de un solo uso, y la contraseña la
    // elige el usuario recién al confirmar (AuthService.restablecerContraseniaConToken).
    public void enviarLinkRestablecimiento(String destinatario, String nombreUsuario, String link) {
        try {
            String texto = "¡Hola, " + nombreUsuario + "!\n\n" +
                    "Solicitaste restablecer tu contraseña.\n" +
                    "Si fuiste tú, ingresa a este enlace (válido por 30 minutos):\n" +
                    link + "\n\n" +
                    "Si no fuiste tú, ignora este mensaje: tu contraseña actual sigue funcionando.";
            String html = renderizarPlantilla(PLANTILLA_RESTABLECIMIENTO_LINK, Map.of(
                    "NOMBRE_USUARIO", nombreUsuario,
                    "URL_RESTABLECIMIENTO", link
            ));
            enviarEmailBrevo(destinatario, "Restablecer tu contraseña - SIENEP", texto, html);
            log.info("[AUDIT] Email de restablecimiento (link) enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("[AUDIT] Error al enviar email de restablecimiento a {}: {}", destinatario, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el email a " + destinatario);
        }
    }

    // RF15 — notificación del ID de negocio al funcionario creador de la instancia.
    // A diferencia de los flujos de contraseña, un fallo de envío NO debe interrumpir
    // la operación principal: la instancia ya fue persistida correctamente.
    // Por eso este método registra el error como warn y no relanza la excepción.
    public void enviarNotificacionInstancia(String destinatario, String nombreUsuario,
                                            String idNegInstancia, String tituloInstancia,
                                            LocalDateTime fechaHora) {
        try {
            String fechaHoraFormateada = fechaHora.format(FORMATTER_FECHA_HORA);
            String texto = "¡Hola, " + nombreUsuario + "!\n\n" +
                    "Se registró una nueva instancia en el sistema.\n\n" +
                    "ID: " + idNegInstancia + "\n" +
                    "Título: " + tituloInstancia + "\n" +
                    "Fecha y hora: " + fechaHoraFormateada + "\n\n" +
                    "Puedes consultarla en el sistema SIENEP.";
            String html = renderizarPlantilla(PLANTILLA_NOTIFICACION_INSTANCIA, Map.of(
                    "NOMBRE_USUARIO", nombreUsuario,
                    "ID_NEG_INSTANCIA", idNegInstancia,
                    "TITULO_INSTANCIA", tituloInstancia,
                    "FECHA_HORA", fechaHoraFormateada,
                    "URL_INICIO_SESION", frontendUrl
            ));
            enviarEmailBrevo(destinatario, "Nueva instancia registrada - " + idNegInstancia, texto, html);
            log.info("[AUDIT] Notificación de instancia {} enviada a {}", idNegInstancia, destinatario);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar notificación de instancia {} a {}: {}",
                    idNegInstancia, destinatario, e.getMessage());
        }
    }

    // RF23 — notificación por email al responsable cuando se le asigna un recordatorio.
    // Mismo criterio que RF15: un fallo de envío no debe revertir la creación del
    // recordatorio, que ya fue persistido (y, si corresponde, sincronizado con Calendar).
    public void enviarNotificacionRecordatorio(String destinatario, String nombreUsuario,
                                               String idNegRecordatorio, String tituloRecordatorio,
                                               LocalDateTime fechaHora) {
        try {
            String fechaHoraFormateada = fechaHora.format(FORMATTER_FECHA_HORA);
            String texto = "¡Hola, " + nombreUsuario + "!\n\n" +
                    "Se te asignó un nuevo recordatorio en el sistema.\n\n" +
                    "ID: " + idNegRecordatorio + "\n" +
                    "Título: " + tituloRecordatorio + "\n" +
                    "Fecha y hora: " + fechaHoraFormateada + "\n\n" +
                    "Puedes consultarlo en el sistema SIENEP.";
            String html = renderizarPlantilla(PLANTILLA_NOTIFICACION_RECORDATORIO, Map.of(
                    "NOMBRE_USUARIO", nombreUsuario,
                    "ID_NEG_RECORDATORIO", idNegRecordatorio,
                    "TITULO_RECORDATORIO", tituloRecordatorio,
                    "FECHA_HORA", fechaHoraFormateada,
                    "URL_INICIO_SESION", frontendUrl
            ));
            enviarEmailBrevo(destinatario, "Nuevo recordatorio asignado - " + idNegRecordatorio, texto, html);
            log.info("[AUDIT] Notificación de recordatorio {} enviada a {}", idNegRecordatorio, destinatario);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar notificación de recordatorio {} a {}: {}",
                    idNegRecordatorio, destinatario, e.getMessage());
        }
    }

    // RF23 — aviso de proximidad, disparado por RecordatorioNotificacionScheduler (cron propio).
    // Distinto del método anterior: ese confirma que el recordatorio fue creado;
    // este avisa que está por vencer (dentro de las próximas 24hs).
    //
    // No es redundante con RF22 (sync a Google Calendar) pese a perseguir el mismo fin: este
    // es un aviso first-party por correo/WhatsApp que SIENEP dispara directamente, mientras
    // que cualquier aviso de Calendar es un efecto lateral de Google (depende de que la sync
    // haya funcionado y de que el responsable tenga ese calendario agregado). RF22 y RF23 son
    // requerimientos separados en la especificación, no uno sustituto del otro.
    public void enviarRecordatorioProximo(String destinatario, String nombreUsuario,
                                          String idNegRecordatorio, String tituloRecordatorio,
                                          LocalDateTime fechaHora) {
        try {
            String fechaHoraFormateada = fechaHora.format(FORMATTER_FECHA_HORA);
            String texto = "¡Hola, " + nombreUsuario + "!\n\n" +
                    "Tienes un recordatorio próximo a vencer en el sistema SIENEP.\n\n" +
                    "ID: " + idNegRecordatorio + "\n" +
                    "Título: " + tituloRecordatorio + "\n" +
                    "Fecha y hora: " + fechaHoraFormateada + "\n\n" +
                    "Recuerda revisarlo antes de que venza.";
            String html = renderizarPlantilla(PLANTILLA_RECORDATORIO_PROXIMO, Map.of(
                    "NOMBRE_USUARIO", nombreUsuario,
                    "ID_NEG_RECORDATORIO", idNegRecordatorio,
                    "TITULO_RECORDATORIO", tituloRecordatorio,
                    "FECHA_HORA", fechaHoraFormateada,
                    "URL_INICIO_SESION", frontendUrl
            ));
            enviarEmailBrevo(destinatario, "Recordatorio próximo a vencer - " + idNegRecordatorio, texto, html);
            log.info("[AUDIT] Aviso de proximidad de recordatorio {} enviado a {}", idNegRecordatorio, destinatario);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar aviso de proximidad de recordatorio {} a {}: {}",
                    idNegRecordatorio, destinatario, e.getMessage());
        }
    }
}
