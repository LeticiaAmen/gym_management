package com.gym.gym_management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio simple para envío de correos transaccionales (texto plano) para recordatorios.
 * Versión mínima: sólo construye y envía un email de recordatorio de vencimiento.
 */
@Service
public class EmailService {

    /** Asunto usado en los recordatorios de pago próximo a vencer. */
    public static final String SUBJECT_REMINDER = "Recordatorio: tu abono vence pronto";

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:}") String configuredFrom,
                        @Value("${spring.mail.username:}") String defaultUser) {
        this.mailSender = mailSender;
        this.from = (configuredFrom == null || configuredFrom.isBlank()) ? defaultUser : configuredFrom;
    }

    /**
     * Envía un correo de texto plano avisando que el pago vence en N días.
     * @param to email del cliente
     * @param clientName nombre completo o parcial del cliente
     * @param expirationDate fecha de vencimiento formateada (dd/MM/yyyy)
     */
    public void sendExpirationReminder(@NonNull String to,
                                       @NonNull String clientName,
                                       @NonNull String expirationDate) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(SUBJECT_REMINDER);
        String body = "Hola " + clientName + ",\n\n" +
                "Tu abono vence el " + expirationDate + ".\n" +
                "Renová antes de esa fecha para no perder acceso.\n\n" +
                "Gracias,\nEquipo del Gimnasio";
        msg.setText(body);
        mailSender.send(msg);
    }
}
