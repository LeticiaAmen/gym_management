package com.gym.gym_management.model;

public enum PaymentState {
    UP_TO_DATE, // Pago al día (vigente)
    EXPIRED,    // Membresía vencida (expirationDate < hoy, no anulado)
    VOIDED      // Pago anulado
}
