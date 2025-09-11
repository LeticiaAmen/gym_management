package com.gym.gym_management.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(prefix = "app.email", name = "enabled", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaymentService paymentService;

    public EmailService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Recordatorios automáticos (no-op: solo logs si emails deshabilitados)
    @Scheduled(cron = "0 0 9 * * ?") // 9:00 AM todos los días
    public void sendPaymentReminders() {
        paymentService.findExpiringPayments().forEach(payment -> {
            try {
                sendPaymentReminder(
                    payment.getClient().getEmail(),
                    payment.getClient().getFirstName(),
                    payment.getAmount(),
                    payment.getExpirationDate()
                );
                logger.info("[EmailService] Recordatorio (simulado) para pago ID: {}", payment.getId());
            } catch (Exception e) {
                logger.error("[EmailService] Error simulando recordatorio para pago ID: {}", payment.getId(), e);
            }
        });
    }

    @Scheduled(cron = "0 0 10 * * ?") // 10:00 AM todos los días
    public void sendOverdueNotifications() {
        paymentService.findOverduePayments().forEach(payment -> {
            try {
                sendOverdueNotification(
                    payment.getClient().getEmail(),
                    payment.getClient().getFirstName(),
                    payment.getAmount(),
                    payment.getExpirationDate()
                );
                logger.info("[EmailService] Vencido (simulado) para pago ID: {}", payment.getId());
            } catch (Exception e) {
                logger.error("[EmailService] Error simulando vencido para pago ID: {}", payment.getId(), e);
            }
        });
    }

    // Métodos de envío (no-op): solo loggean el contenido del mensaje
    public void sendPaymentReminder(String toEmail, String clientName, Double amount, LocalDate expirationDate) {
        logger.debug("Simulando envío de recordatorio a {} | {} | ${} | vence {}",
                toEmail, clientName, amount, expirationDate != null ? expirationDate.format(DATE_FORMAT) : "-");
    }

    public void sendOverdueNotification(String toEmail, String clientName, Double amount, LocalDate expirationDate) {
        logger.debug("Simulando notificación de vencido a {} | {} | ${} | venció {}",
                toEmail, clientName, amount, expirationDate != null ? expirationDate.format(DATE_FORMAT) : "-");
    }
}
