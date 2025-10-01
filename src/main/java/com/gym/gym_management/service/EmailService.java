package com.gym.gym_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio para el envío de correos electrónicos.
 * <p>
 * Este servicio proporciona métodos para enviar correos electrónicos desde la aplicación.
 * Se utiliza principalmente para la recuperación de contraseñas y notificaciones.
 *
 * @author GymManagement
 * @version 1.0
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.mail.from:no-reply@gymmanagement.com}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo electrónico con el contenido especificado.
     * <p>
     * Si el envío de correos está desactivado en la configuración, solo registrará un mensaje
     * en los logs en lugar de enviarlo realmente.
     *
     * @param to destinatario del correo
     * @param subject asunto del correo
     * @param text contenido del correo (puede ser HTML)
     * @param isHtml indica si el contenido está en formato HTML
     * @return true si el correo se envió correctamente o se registró (cuando está desactivado)
     */
    public boolean sendEmail(String to, String subject, String text, boolean isHtml) {
        if (!emailEnabled) {
            // Si el envío de correos está desactivado, solo registramos el contenido
            logger.info("Email sending is disabled. Would have sent email to: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("Content: {}", text);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, isHtml);
            helper.setFrom(fromEmail);

            mailSender.send(message);
            logger.info("Email sent to: {}", to);
            return true;
        } catch (MessagingException e) {
            logger.error("Error sending email to: {}", to, e);
            return false;
        }
    }

    /**
     * Envía un recordatorio de vencimiento de pago a un cliente.
     * <p>
     * Este método crea y envía un correo electrónico formateado para notificar
     * al cliente que su pago está próximo a vencer.
     *
     * @param clientEmail correo electrónico del cliente
     * @param clientName nombre del cliente
     * @param expirationDate fecha de vencimiento formateada
     * @return true si el correo se envió correctamente o se registró (cuando está desactivado)
     */
    public boolean sendExpirationReminder(String clientEmail, String clientName, String expirationDate) {
        String subject = "Recordatorio: Tu membresía del gimnasio vence pronto";
        String htmlContent = createExpirationReminderContent(clientName, expirationDate);

        return sendEmail(clientEmail, subject, htmlContent, true);
    }

    /**
     * Crea el contenido HTML para el correo de recordatorio de vencimiento.
     *
     * @param clientName nombre del cliente
     * @param expirationDate fecha de vencimiento formateada
     * @return contenido HTML del correo
     */
    private String createExpirationReminderContent(String clientName, String expirationDate) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "    <style>" +
               "        body { font-family: Arial, sans-serif; line-height: 1.6; }" +
               "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
               "        .header { background-color: #1a1d29; color: white; padding: 15px; text-align: center; }" +
               "        .content { padding: 20px; }" +
               "        .warning { background-color: #fff3cd; color: #856404; padding: 15px; border-radius: 4px; margin: 15px 0; }" +
               "        .footer { font-size: 12px; color: #666; padding-top: 20px; }" +
               "    </style>" +
               "</head>" +
               "<body>" +
               "    <div class='container'>" +
               "        <div class='header'>" +
               "            <h2>🏋️ GYM BOX - Recordatorio de Membresía</h2>" +
               "        </div>" +
               "        <div class='content'>" +
               "            <p>Hola <strong>" + clientName + "</strong>,</p>" +
               "            <div class='warning'>" +
               "                <strong>⚠️ Tu membresía está próxima a vencer</strong><br>" +
               "                <p>Tu membresía del gimnasio vencerá el <strong>" + expirationDate + "</strong>.</p>" +
               "            </div>" +
               "            <p>Para continuar disfrutando de nuestras instalaciones y servicios, " +
               "               te recomendamos renovar tu membresía antes de la fecha de vencimiento.</p>" +
               "            <p><strong>¿Cómo renovar?</strong></p>" +
               "            <ul>" +
               "                <li>Visita nuestra recepción en el gimnasio</li>" +
               "                <li>Llama al teléfono de contacto</li>" +
               "                <li>Habla con nuestro personal de atención al cliente</li>" +
               "            </ul>" +
               "            <p>¡Gracias por ser parte de nuestra familia GYM BOX! 💪</p>" +
               "        </div>" +
               "        <div class='footer'>" +
               "            <p>Este es un correo automático, por favor no respondas a este mensaje.</p>" +
               "            <p>&copy; 2024 GYM BOX. Todos los derechos reservados.</p>" +
               "        </div>" +
               "    </div>" +
               "</body>" +
               "</html>";
    }
}
