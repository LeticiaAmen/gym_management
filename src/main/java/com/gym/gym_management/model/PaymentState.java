package com.gym.gym_management.model;

/** Estados de negocio que puede tener una membresía o un pago. */
public enum PaymentState {
    UP_TO_DATE, // Pago al día (vigente)
    EXPIRED,    // Membresía vencida (expirationDate < hoy, no anulado)
    VOIDED      // Pago anulado
}
