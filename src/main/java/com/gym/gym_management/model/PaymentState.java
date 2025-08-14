package com.gym.gym_management.model;
/**
 * Enum que representa el estado actual de un pago.
 *
 * Valores:
 * - PENDING    → El pago está pendiente de realizarse.
 * - UP_TO_DATE → El pago está al día (mensualidad abonada).
 * - EXPIRED    → El pago ha vencido (mensualidad atrasada).
 *
 * Relación con los requerimientos:
 * - "Historial de Pagos": permite mostrar al cliente si su mensualidad está pendiente o vencida.
 * - "Recordatorios Automáticos de Pago": el estado puede usarse para disparar notificaciones.
 * - "Panel de Administrador": facilita filtrar pagos atrasados o pendientes para seguimiento.
 */
public enum PaymentState {
    PENDING,
    UP_TO_DATE,
    EXPIRED
}
