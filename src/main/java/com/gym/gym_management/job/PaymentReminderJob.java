package com.gym.gym_management.job;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Job programado que envía recordatorios de renovación.
 * Opción A: cada Payment representa un período ya pagado; se recuerda la renovación
 * daysBefore días antes de su expirationDate (sin usar estados PENDING).
 */
@Component
public class PaymentReminderJob {

    private final IPaymentRepository paymentRepository;
    private final EmailService emailService;
    private final boolean enabled;
    private final int daysBefore;
    private final boolean logEnabled;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PaymentReminderJob(IPaymentRepository paymentRepository,
                               EmailService emailService,
                               @Value("${app.reminder.enabled:true}") boolean enabled,
                               @Value("${app.reminder.daysBefore:3}") int daysBefore,
                               @Value("${app.reminder.log:false}") boolean logEnabled) {
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.enabled = enabled;
        this.daysBefore = daysBefore;
        this.logEnabled = logEnabled;
    }

    /** Ejecuta el recordatorio (cron ajustable). */
    @Scheduled(cron = "0 * * * * *") // pruebas: cada minuto. Producción: 0 0 9 * * *
    @Transactional(readOnly = true)
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
            String clientName = buildName(p);
            emailService.sendExpirationReminder(email, clientName, p.getExpirationDate().format(FMT));
            if (logEnabled) {
                System.out.println("[PaymentReminderJob] enviado a=" + email + " vence=" + p.getExpirationDate());
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
