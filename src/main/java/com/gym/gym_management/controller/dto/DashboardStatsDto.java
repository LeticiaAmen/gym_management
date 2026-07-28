package com.gym.gym_management.controller.dto;

/** Resumen de contadores que necesita la pantalla principal del administrador. */
public class DashboardStatsDto {
    private final long activeClients;
    private final long expiredPayments;

    public DashboardStatsDto(long activeClients, long expiredPayments) {
        this.activeClients = activeClients;
        this.expiredPayments = expiredPayments;
    }

    public long getActiveClients() {
        return activeClients;
    }

    public long getExpiredPayments() {
        return expiredPayments;
    }
}
