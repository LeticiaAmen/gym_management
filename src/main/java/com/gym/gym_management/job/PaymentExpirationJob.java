package com.gym.gym_management.job;

import com.gym.gym_management.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado que materializa el estado EXPIRED en la base de datos.
 * <p>
 * Cada día ejecuta un bulk update que convierte todos los pagos UP_TO_DATE cuya
 * expirationDate ya pasó (y no están anulados) a EXPIRED. Persistir el estado permite:
 * <ul>
 *   <li>Reportes y métricas rápidas (sin recalcular por fecha en cada consulta).</li>
 *   <li>Integraciones futuras (control de acceso físico según estado).</li>
 *   <li>Evitar condiciones de carrera dispersas (la transición se centraliza aquí).</li>
 * </ul>
 * El método del servicio es idempotente: ejecutarlo varias veces el mismo día no genera efectos extra.
 */
@Component
public class PaymentExpirationJob {
    private final PaymentService paymentService;
    private final boolean enabled;

    /**
     * @param paymentService servicio de pagos
     * @param enabled flag configurable (app.expirationJob.enabled) para activar/desactivar sin tocar código
     */
    public PaymentExpirationJob(PaymentService paymentService,
                                @Value("${app.expirationJob.enabled:true}") boolean enabled) {
        this.paymentService = paymentService;
        this.enabled = enabled;
    }

    /**
     * Ejecuta el proceso de expiración diariamente a las 02:00 AM.
     * Cron: segundo minuto hora díaMes mes díaSemana
     */
    @Scheduled(cron = "0 0 2 * * *") //0 0 2 * * * A los 2 am
    public void run() {
        if (!enabled) return;
        int updated = paymentService.expireOverduePayments();
        if (updated > 0) {
            System.out.println("[PaymentExpirationJob] Pagos marcados como EXPIRED: " + updated);
        }
    }
}
