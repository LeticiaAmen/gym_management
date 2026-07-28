package com.gym.gym_management.service;

/**
 * Excepción lanzada cuando se excede el límite de intentos permitidos para
 * una operación sensible (login o recuperación de contraseña).
 * <p>
 * Se maneja globalmente devolviendo HTTP 429 (Too Many Requests) para
 * desalentar ataques de fuerza bruta o enumeración de usuarios.
 */
/** Excepción de dominio que indica que un cliente superó el límite de intentos. */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}

