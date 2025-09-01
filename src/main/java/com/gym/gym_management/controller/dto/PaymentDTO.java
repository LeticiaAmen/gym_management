package com.gym.gym_management.controller.dto;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;

import java.time.LocalDate;

/**
 * Data Transfer Object para la entidad Payment
 * Contiene solo la información que debe ser expuesta a través de la API
 */
public class PaymentDTO {
    private Long id;
    private LocalDate paymentDate;
    private LocalDate expirationDate;
    private Double amount;
    private PaymentState paymentState;
    private Long clientId;

    public PaymentDTO(){
    }

    //getters y setters
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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    /**
     * Crea un PaymentDTO desde una entidad Payment
     *
     * @param payment para convertir
     * @return el dto con la información de la entidad o null si payment es null
     */
    public static PaymentDTO fromEntity(Payment payment) {
        if(payment == null) {
            return null;
        }
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setExpirationDate(payment.getExpirationDate());
        dto.setAmount(payment.getAmount());
        dto.setPaymentState(payment.getPaymentState());
        if (payment.getClient() != null) {
           dto.setClientId(payment.getClient().getId());
        }
        return dto;
    }
}
