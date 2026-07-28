package com.gym.gym_management.model;

/** Valores permitidos para identificar cómo se realizó un pago. */
public enum PaymentMethod {
    CASH,       // Efectivo
    DEBIT,      // Débito
    CREDIT,     // Crédito
    TRANSFER,   // Transferencia bancaria
    OTHER       // Otros métodos
}
