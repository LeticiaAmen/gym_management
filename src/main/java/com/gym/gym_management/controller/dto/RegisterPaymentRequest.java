package com.gym.gym_management.controller.dto;

import com.gym.gym_management.model.PaymentMethod;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class RegisterPaymentRequest {
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clientId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private Double amount;

    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod method;

    private LocalDate paymentDate;

    // En meses, para calcular la fecha de vencimiento
    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración mínima es 1 mes")
    private Integer duration;

    // Getters y setters
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
