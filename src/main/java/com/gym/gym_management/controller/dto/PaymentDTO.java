package com.gym.gym_management.controller.dto;

import com.gym.gym_management.model.PaymentMethod;
import com.gym.gym_management.model.PaymentState;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class PaymentDTO {
    private Long id;

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clientId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private Double amount;

    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod method;

    @NotNull(message = "El mes es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    private Integer month;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 2023, message = "El año debe ser válido")
    private Integer year;

    private LocalDate paymentDate;
    private LocalDate expirationDate; // calculada: 30 días (mensual) o durationDays

    // Opcional: duración personalizada en días. Si es null, se asume 30 días
    @Min(value = 1, message = "La duración debe ser al menos 1 día")
    private Integer durationDays;

    private PaymentState state;
    private boolean voided;
    private Long voidedBy;
    private String voidReason;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public PaymentState getState() { return state; }
    public void setState(PaymentState state) { this.state = state; }

    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }

    public Long getVoidedBy() { return voidedBy; }
    public void setVoidedBy(Long voidedBy) { this.voidedBy = voidedBy; }

    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
}
