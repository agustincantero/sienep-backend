package com.fides.sienep.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public void enviarContraseniaTemporal(String destinatario, String nombreUsuario, String contrasenia) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sienep.app");
            message.setTo(destinatario);
            message.setSubject("Acceso al sistema - Contraseña temporal");
            message.setText(
                    "¡Hola, " + nombreUsuario + "!\n\n" +
                            "Tu cuenta fue creada con éxito.\n" +
                            "Tu contraseña temporal es: " + contrasenia + "\n\n" +
                            "Por seguridad, cámbiala en tu primer ingreso."
            );
            mailSender.send(message);
            log.info("[AUDIT] Email de contraseña temporal enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("[AUDIT] Error al enviar email a {}: {}", destinatario, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el email a " + destinatario);
        }
    }

    public void enviarContraseniaRestablecida(String destinatario, String nombreUsuario, String contrasenia) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sienep.app");
            message.setTo(destinatario);
            message.setSubject("Restablecimiento de contraseña - SIENEP");
            message.setText(
                    "¡Hola, " + nombreUsuario + "!\n\n" +
                            "Se generó una nueva contraseña para tu cuenta.\n" +
                            "Tu contraseña temporal es: " + contrasenia + "\n\n" +
                            "Por seguridad, cámbiala en tu próximo ingreso."
            );
            mailSender.send(message);
            log.info("[AUDIT] Email de restablecimiento enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("[AUDIT] Error al enviar email a {}: {}", destinatario, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el email a " + destinatario);
        }
    }

    // Flujo "olvidé mi contraseña": a diferencia de enviarContraseniaRestablecida
    // (usada cuando un admin autenticado resetea la de otro), acá no se manda ninguna
    // contraseña — se manda un link con un token de un solo uso, y la contraseña la
    // elige el usuario recién al confirmar (AuthService.restablecerContraseniaConToken).
    public void enviarLinkRestablecimiento(String destinatario, String nombreUsuario, String link) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sienep.app");
            message.setTo(destinatario);
            message.setSubject("Restablecer tu contraseña - SIENEP");
            message.setText(
                    "¡Hola, " + nombreUsuario + "!\n\n" +
                            "Solicitaste restablecer tu contraseña.\n" +
                            "Si fuiste vos, entrá a este link (válido por 30 minutos):\n" +
                            link + "\n\n" +
                            "Si no fuiste vos, ignorá este mensaje: tu contraseña actual sigue funcionando."
            );
            mailSender.send(message);
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
                                            String idNegInstancia, String tituloInstancia) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sienep.app");
            message.setTo(destinatario);
            message.setSubject("Nueva instancia registrada - " + idNegInstancia);
            message.setText(
                    "¡Hola, " + nombreUsuario + "!\n\n" +
                            "Se registró una nueva instancia en el sistema.\n\n" +
                            "ID: " + idNegInstancia + "\n" +
                            "Título: " + tituloInstancia + "\n\n" +
                            "Podés consultarla en el sistema SIENEP."
            );
            mailSender.send(message);
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sienep.app");
            message.setTo(destinatario);
            message.setSubject("Nuevo recordatorio asignado - " + idNegRecordatorio);
            message.setText(
                    "¡Hola, " + nombreUsuario + "!\n\n" +
                            "Se te asignó un nuevo recordatorio en el sistema.\n\n" +
                            "ID: " + idNegRecordatorio + "\n" +
                            "Título: " + tituloRecordatorio + "\n" +
                            "Fecha y hora: " + fechaHora + "\n\n" +
                            "Podés consultarlo en el sistema SIENEP."
            );
            mailSender.send(message);
            log.info("[AUDIT] Notificación de recordatorio {} enviada a {}", idNegRecordatorio, destinatario);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar notificación de recordatorio {} a {}: {}",
                    idNegRecordatorio, destinatario, e.getMessage());
        }
    }

    // RF23 — aviso de proximidad, disparado por RecordatorioNotificacionScheduler (cron propio).
    // Distinto del método anterior: ese confirma que el recordatorio fue creado;
    // este avisa que está por vencer (dentro de las próximas 24hs).
    public void enviarRecordatorioProximo(String destinatario, String nombreUsuario,
                                          String idNegRecordatorio, String tituloRecordatorio,
                                          LocalDateTime fechaHora) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sienep.app");
            message.setTo(destinatario);
            message.setSubject("Recordatorio próximo a vencer - " + idNegRecordatorio);
            message.setText(
                    "¡Hola, " + nombreUsuario + "!\n\n" +
                            "Tenés un recordatorio próximo a vencer en el sistema SIENEP.\n\n" +
                            "ID: " + idNegRecordatorio + "\n" +
                            "Título: " + tituloRecordatorio + "\n" +
                            "Fecha y hora: " + fechaHora + "\n\n" +
                            "Recordá revisarlo antes de que venza."
            );
            mailSender.send(message);
            log.info("[AUDIT] Aviso de proximidad de recordatorio {} enviado a {}", idNegRecordatorio, destinatario);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar aviso de proximidad de recordatorio {} a {}: {}",
                    idNegRecordatorio, destinatario, e.getMessage());
        }
    }
}