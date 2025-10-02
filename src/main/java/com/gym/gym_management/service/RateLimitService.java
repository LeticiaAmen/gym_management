package com.gym.gym_management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio simple en memoria para aplicar rate limiting a endpoints sensibles
 * como login y recuperación de contraseña.
 * <p>
 * Estrategia:
 * - Se mantiene un registro (key → intentos, bloqueos) en mapas concurrentes.
 * - Las claves combinan email + ip para reducir riesgo de bloqueo global.
 * - Al exceder el máximo de intentos fallidos se bloquea hasta la hora indicada.
 * - Tras un éxito (login correcto) se limpian los contadores.
 * <p>
 * NOTA: Implementación temporal en memoria. Para producción se recomienda
 * usar Redis / Bucket4j con persistencia distribuida.
 */
@Service
public class RateLimitService {

    @Value("${security.ratelimit.login.maxAttempts:5}")
    private int loginMaxAttempts;

    @Value("${security.ratelimit.login.blockMinutes:10}")
    private int loginBlockMinutes;

    @Value("${security.ratelimit.passwordReset.maxAttempts:5}")
    private int resetMaxAttempts;

    @Value("${security.ratelimit.passwordReset.blockMinutes:15}")
    private int resetBlockMinutes;

    /**
     * Registro de intentos de login (clave: email|ip)
     */
    private final Map<String, AttemptRecord> loginAttempts = new ConcurrentHashMap<>();

    /**
     * Registro de solicitudes de password reset (clave: email|ip)
     */
    private final Map<String, AttemptRecord> resetAttempts = new ConcurrentHashMap<>();

    /**
     * Verifica si aún está permitido intentar login para la combinación email+ip.
     * Lanza excepción si la clave está bloqueada.
     * @param email correo (puede ser null o vacío - se normaliza)
     * @param ip dirección IP remota
     */
    public void assertLoginAllowed(String email, String ip) {
        String key = buildKey(email, ip);
        AttemptRecord record = loginAttempts.get(key);
        if (record != null && record.blockedUntil != null && LocalDateTime.now().isBefore(record.blockedUntil)) {
            throw new RateLimitExceededException("Demasiados intentos fallidos de login. Intenta nuevamente más tarde.");
        }
    }

    /**
     * Registra un intento fallido de login. Si supera el máximo, establece bloqueo.
     */
    public void registerLoginFailure(String email, String ip) {
        String key = buildKey(email, ip);
        AttemptRecord record = loginAttempts.computeIfAbsent(key, k -> new AttemptRecord());
        record.failedAttempts++;
        if (record.failedAttempts >= loginMaxAttempts) {
            record.blockedUntil = LocalDateTime.now().plusMinutes(loginBlockMinutes);
        }
    }

    /**
     * Registra un login exitoso: limpia contadores y bloqueos previos.
     */
    public void registerLoginSuccess(String email, String ip) {
        String key = buildKey(email, ip);
        loginAttempts.remove(key);
    }

    /**
     * Verifica si es posible solicitar un nuevo password reset para email+ip.
     * Si está bloqueado lanza excepción.
     */
    public void assertPasswordResetAllowed(String email, String ip) {
        String key = buildKey(email, ip);
        AttemptRecord record = resetAttempts.get(key);
        if (record != null && record.blockedUntil != null && LocalDateTime.now().isBefore(record.blockedUntil)) {
            throw new RateLimitExceededException("Se han realizado demasiadas solicitudes de recuperación. Intenta nuevamente más tarde.");
        }
    }

    /**
     * Registra una solicitud de password reset. Se cuenta tanto si el email existe como si no,
     * para evitar enumeración de usuarios.
     */
    public void registerPasswordResetRequest(String email, String ip) {
        String key = buildKey(email, ip);
        AttemptRecord record = resetAttempts.computeIfAbsent(key, k -> new AttemptRecord());
        record.failedAttempts++; // semánticamente son 'solicitudes acumuladas'
        if (record.failedAttempts >= resetMaxAttempts) {
            record.blockedUntil = LocalDateTime.now().plusMinutes(resetBlockMinutes);
        }
    }

    /** Uso interno para formar clave estable (case insensitive en email). */
    private String buildKey(String email, String ip) {
        String normalized = email == null ? "(null)" : email.trim().toLowerCase();
        return normalized + "|" + Objects.toString(ip, "?");
    }

    /**
     * Registro interno de intentos.
     */
    private static class AttemptRecord {
        int failedAttempts = 0;
        LocalDateTime blockedUntil;
    }

    public void clearAll() { // Uso exclusivo en tests para aislar casos
        loginAttempts.clear();
        resetAttempts.clear();
    }
}
