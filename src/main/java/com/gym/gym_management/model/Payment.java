package com.gym.gym_management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un pago realizado por un cliente del gimnasio.
 *
 * Características principales:
 * - Tiene relación N:1 con Client (un cliente puede tener muchos pagos).
 * - Contiene información sobre fecha de pago, fecha de vencimiento, monto y estado del pago.
 * - Es el lado propietario de la relación con Client (posee la FK client_id).
 *
 * Relación con los requerimientos:
 * - "Historial de Pagos" (Clientes): permite consultar pagos anteriores y verificar si la mensualidad está vencida.
 * - "Panel de Administrador" (Administradores): permite registrar y gestionar pagos.
 * - "Recordatorios Automáticos de Pago": el campo expirationDate puede usarse para enviar notificaciones antes y después del vencimiento:contentReference[oaicite:1]{index=1}.
 */
@Entity
@Table(name = "payments")
public class Payment {

    /**
     * Identificador único del pago.
     * Generado automáticamente con estrategia IDENTITY (autoincremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Fecha en que se realizó el pago
    private LocalDate paymentDate;

    //Fecha de vencimiento del pago
    private LocalDate expirationDate;

    // monto pagado
    private Double amount;

    // Método de pago
    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 20)
    private PaymentMethod method;

    // Período informado por el pago (mes/año) - evitar palabras reservadas en H2
    @Column(name = "period_month")
    private Integer month;

    @Column(name = "period_year")
    private Integer year;

    /** Duración personalizada en días (opcional). Si es null, se asume mensual (1 mes). */
    @Column(name = "duration_days")
    private Integer durationDays;

    /**
     * Estado del pago (ej: PENDIENTE, PAGADO, VENCIDO).
     * @Enumerated sin tipo explícito usa ORDINAL por defecto, pero es recomendable usar STRING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_state", length = 20)
    private PaymentState paymentState;

    /**
     * Relación N:1 con Client.
     * - fetch = LAZY: la información del cliente se carga solo cuando se necesita.
     * - @JoinColumn: client_id es la FK en la tabla payments.
     * - nullable = false: un pago siempre pertenece a un cliente.
     */
    // Relación bidireccional: Payment es el lado propietario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore //evitamos exponer toda la info del cliente asociado y evitar ciclos recursivos con la lista de payments en cliente
    private Client client;

    // Campos para anulación
    private boolean voided = false;
    private Long voidedBy;
    private LocalDateTime voidedAt;
    private String voidReason;

    @PrePersist
    protected void onCreate() {
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
        if (expirationDate == null) {
            // Si se indicó duración por días, usarla; de lo contrario, mensual
            if (durationDays != null && durationDays > 0) {
                expirationDate = paymentDate.plusDays(durationDays);
            } else {
                expirationDate = paymentDate.plusMonths(1);
            }
        }
    }

    public Payment() {
    }

    public Payment(Client client, Double amount, PaymentMethod method, Integer month, Integer year) {
        this.client = client;
        this.amount = amount;
        this.method = method;
        this.month = month;
        this.year = year;
        this.paymentDate = LocalDate.now();
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentState getState() {
        return paymentState;
    }

    public void setState(PaymentState state) {
        this.paymentState = state;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public Long getVoidedBy() {
        return voidedBy;
    }

    public void setVoidedBy(Long voidedBy) {
        this.voidedBy = voidedBy;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }

    public void setVoidedAt(LocalDateTime voidedAt) {
        this.voidedAt = voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public void anularPago(Long adminId, String reason) {
        this.voided = true;
        this.voidedBy = adminId;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
        this.paymentState = PaymentState.VOIDED;
    }
}
