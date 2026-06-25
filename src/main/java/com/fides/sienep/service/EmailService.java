package com.fides.sienep.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
            throw new RuntimeException("No se pudo enviar el email a " + destinatario);
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
            throw new RuntimeException("No se pudo enviar el email a " + destinatario);
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
}