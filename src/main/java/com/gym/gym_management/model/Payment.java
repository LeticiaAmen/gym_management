package com.gym.gym_management.model;

import jakarta.persistence.*;

import java.time.LocalDate;


@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate paymentDate;
    private LocalDate expirationDate;
    private Double amount;
    @Enumerated
    private PaymentState paymentState;

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
