package com.gym.gym_management.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que registra las notificaciones enviadas a los clientes.
 * <p>
 * Esta entidad permite llevar un registro de todos los recordatorios de vencimiento
 * enviados a los clientes, evitando duplicados y proporcionando trazabilidad.
 *
 * @author GymManagement
 * @version 1.0
 */
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del pago al cual se refiere la notificación.
     */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    /**
     * Email del cliente al que se envió la notificación.
     */
    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    /**
     * Tipo de notificación enviada.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    /**
     * Fecha y hora en que se envió la notificación.
     */
    @Column(name = "sent_date", nullable = false)
    private LocalDateTime sentDate;

    /**
     * Estado del envío de la notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    /**
     * Días de anticipación con que se envió el recordatorio.
     */
    @Column(name = "reminder_days")
    private Integer reminderDays;

    // Constructors
    public NotificationLog() {}

    public NotificationLog(Long paymentId, String clientEmail, NotificationType notificationType,
                          NotificationStatus status, Integer reminderDays) {
        this.paymentId = paymentId;
        this.clientEmail = clientEmail;
        this.notificationType = notificationType;
        this.status = status;
        this.reminderDays = reminderDays;
        this.sentDate = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Integer getReminderDays() {
        return reminderDays;
    }

    public void setReminderDays(Integer reminderDays) {
        this.reminderDays = reminderDays;
    }

    /**
     * Enumeración que define los tipos de notificación disponibles.
     */
    public enum NotificationType {
        EXPIRATION_REMINDER,    // Recordatorio de vencimiento próximo
        OVERDUE_NOTICE,        // Notificación de vencimiento
        RENEWAL_REMINDER       // Recordatorio de renovación
    }

    /**
     * Enumeración que define los estados posibles de una notificación.
     */
    public enum NotificationStatus {
        SENT,     // Enviado correctamente
        FAILED    // Falló el envío
    }
}
