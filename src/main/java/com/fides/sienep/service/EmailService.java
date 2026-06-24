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
}