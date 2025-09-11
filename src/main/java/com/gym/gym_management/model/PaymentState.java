package com.gym.gym_management.model;

public enum PaymentState {
    PENDING,    // Pago pendiente
    UP_TO_DATE, // Pago al día
    EXPIRED,    // Pago vencido
    VOIDED      // Pago anulado
}
