package com.gym.gym_management.service;

/**
 * Excepción lanzada cuando se excede el límite de intentos permitidos para
 * una operación sensible (login o recuperación de contraseña).
 * <p>
 * Se maneja globalmente devolviendo HTTP 429 (Too Many Requests) para
 * desalentar ataques de fuerza bruta o enumeración de usuarios.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}

