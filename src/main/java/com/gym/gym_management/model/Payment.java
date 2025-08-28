package com.gym.gym_management.model;

import jakarta.persistence.*;

import java.time.LocalDate;

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

    /**
     * Estado del pago (ej: PENDIENTE, PAGADO, VENCIDO).
     * @Enumerated sin tipo explícito usa ORDINAL por defecto, pero es recomendable usar STRING.
     */
    @Enumerated(EnumType.STRING)
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
    private Client client;

    public Payment() {
    }

    public Payment(LocalDate paymentDate, LocalDate expirationDate, Double amount, PaymentState paymentState, Client client) {
        this.paymentDate = paymentDate;
        this.expirationDate = expirationDate;
        this.amount = amount;
        this.paymentState = paymentState;
        this.client = client;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentState getPaymentState() {
        return paymentState;
    }

    public void setPaymentState(PaymentState paymentState) {
        this.paymentState = paymentState;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
