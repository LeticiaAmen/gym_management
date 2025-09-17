package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.controller.dto.OverdueClientDTO;
import com.gym.gym_management.controller.dto.ExpiringClientDTO;
import com.gym.gym_management.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * Obtiene lista de clientes con pagos por vencer en los próximos 7 días
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<ExpiringClientDTO>> getExpiringPayments() {
        return ResponseEntity.ok(reportService.getClientsWithPaymentsExpiringSoon());
    }

    /**
     * Obtiene lista de clientes con último pago válido vencido, incluyendo fecha de expiración.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<OverdueClientDTO>> getOverduePayments() {
        return ResponseEntity.ok(reportService.getClientsWithOverduePayments());
    }

    /**
     * Calcula el flujo de caja entre dos fechas
     */
    @GetMapping("/cashflow")
    public ResponseEntity<Double> getCashflow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.calculateCashflow(from, to));
    }
}
