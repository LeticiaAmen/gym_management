package com.gym.gym_management.job;

import com.gym.gym_management.model.NotificationLog;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.INotificationLogRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Job programado que envía recordatorios de renovación.
 * Solo se instancia si app.reminder.enabled=true para facilitar tests sin stub de mail.
 */
@Component
@ConditionalOnProperty(value = "app.reminder.enabled", havingValue = "true")
public class PaymentReminderJob {

    private final IPaymentRepository paymentRepository;
    private final INotificationLogRepository notificationLogRepository;
    private final EmailService emailService;
    private final boolean enabled;
    private final int daysBefore;
    private final boolean logEnabled;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PaymentReminderJob(IPaymentRepository paymentRepository,
                               INotificationLogRepository notificationLogRepository,
                               EmailService emailService,
                               @Value("${app.reminder.enabled:true}") boolean enabled,
                               @Value("${app.reminder.daysBefore:3}") int daysBefore,
                               @Value("${app.reminder.log:false}") boolean logEnabled) {
        this.paymentRepository = paymentRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.emailService = emailService;
        this.enabled = enabled;
        this.daysBefore = daysBefore;
        this.logEnabled = logEnabled;
    }

    /** Ejecuta el recordatorio (cron ajustable). */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendReminders() {
        if (!enabled) return;
        LocalDate target = LocalDate.now().plusDays(daysBefore);
        List<Payment> payments = paymentRepository.findByExpirationDateWithClient(target);
        if (logEnabled) {
            System.out.println("[PaymentReminderJob] target=" + target + " pagosEncontrados=" + payments.size());
        }

        for (Payment p : payments) {
            if (!p.getClient().isActive()) continue;
            String email = p.getClient().getEmail();
            if (email == null || email.isBlank()) continue;

            // Verificar si ya se envió un recordatorio para este pago
            Optional<NotificationLog> existingNotification = notificationLogRepository
                .findByPaymentIdAndNotificationType(p.getId(), NotificationLog.NotificationType.EXPIRATION_REMINDER);

            if (existingNotification.isPresent()) {
                if (logEnabled) {
                    System.out.println("[PaymentReminderJob] Ya se envió recordatorio para payment ID=" + p.getId());
                }
                continue; // Saltar si ya se envió
            }

            String clientName = buildName(p);
            boolean emailSent = emailService.sendExpirationReminder(email, clientName, p.getExpirationDate().format(FMT));

            // Registrar la notificación en el log
            NotificationLog.NotificationStatus status = emailSent ?
                NotificationLog.NotificationStatus.SENT :
                NotificationLog.NotificationStatus.FAILED;

            NotificationLog notificationLog = new NotificationLog(
                p.getId(),
                email,
                NotificationLog.NotificationType.EXPIRATION_REMINDER,
                status,
                daysBefore
            );

            notificationLogRepository.save(notificationLog);

            if (logEnabled) {
                System.out.println("[PaymentReminderJob] enviado a=" + email + " vence=" + p.getExpirationDate() + " status=" + status);
            }
        }
    }

    private String buildName(Payment p) {
        String fn = p.getClient().getFirstName();
        String ln = p.getClient().getLastName();
        String combined = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
        return combined.isEmpty() ? "cliente" : combined;
    }
}
