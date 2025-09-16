package com.gym.gym_management.controller.dto;

import com.gym.gym_management.model.PaymentMethod;
import com.gym.gym_management.model.PaymentState;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) que representa los datos expuestos de un pago.
 * <p>
 * Se utiliza en la capa de controlador para recibir y devolver información sin exponer
 * directamente la entidad JPA Payment. Incluye validaciones básicas con anotaciones
 * de Bean Validation para garantizar coherencia de datos antes de llegar a la capa de servicio.
 * <p>
 * Campos relevantes:
 * <ul>
 *   <li>clientId: identifica al cliente dueño del pago.</li>
 *   <li>amount: monto abonado (positivo).</li>
 *   <li>method: método de pago (enum legible en vez de valores numéricos).</li>
 *   <li>month/year: período al que se asocia el pago (permite idempotencia).</li>
 *   <li>paymentDate: fecha en que se registró el pago (puede venir del usuario o default = hoy).</li>
 *   <li>expirationDate: fecha hasta la cual el pago mantiene vigente la membresía (calculada).</li>
 *   <li>durationDays: alternativa para membresías de duración personalizada (en lugar de mensual).</li>
 *   <li>state: estado lógico (UP_TO_DATE, PENDING, EXPIRED, VOIDED).</li>
 *   <li>voided / voidedBy / voidReason: trazabilidad de anulaciones.</li>
 * </ul>
 */
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
    private LocalDate expirationDate; // calculada según lógica (mensual o duración en días)

    // Opcional: duración personalizada en días. Si es null, se asume mensual (1 mes)
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
