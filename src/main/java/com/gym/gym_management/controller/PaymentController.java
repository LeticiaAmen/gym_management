package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Registra un nuevo pago.
     * Acceso: solo usuarios con rol ADMIN.
     * @param request datos del pago a registrar.
     * @return PaymentDTO con los detalles del pago registrado.
     */
    @PostMapping
    public ResponseEntity<?> registerPayment(@Valid @RequestBody PaymentDTO request) {
        try {
            PaymentDTO created = paymentService.registerPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("cliente no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }
            return ResponseEntity.badRequest().body(msg);
        } catch (IllegalStateException e) {
            // Conflictos de negocio (idempotencia, inactivo, etc.)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Obtiene la lista de pagos filtrados por cliente, fechas y estado.
     * Acceso: solo usuarios con rol ADMIN.
     * @param clientId id del cliente (opcional).
     * @param from fecha de inicio para el filtro (opcional).
     * @param to fecha de fin para el filtro (opcional).
     * @param state estado del pago (opcional).
     * @param pageable parámetros de paginación.
     * @return lista paginada de PaymentDTO que cumplen con los criterios de filtro.
     */
    @GetMapping
    public ResponseEntity<?> getPayments(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String state,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest().body("El parámetro 'from' no puede ser posterior a 'to'");
        }
        PaymentState parsedState = null;
        if (state != null && !state.isBlank()) {
            String s = state.trim().toUpperCase(Locale.ROOT);
            if ("OVERDUE".equals(s)) {
                // Contrato usa OVERDUE; nuestro enum usa EXPIRED
                parsedState = PaymentState.EXPIRED;
            } else {
                try {
                    parsedState = PaymentState.valueOf(s);
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body("Estado inválido. Use UP_TO_DATE | OVERDUE | VOIDED");
                }
            }
        }
        Page<PaymentDTO> page = paymentService.findPayments(clientId, from, to, parsedState, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Anula un pago existente.
     * Acceso: solo usuarios con rol ADMIN.
     * @param id identificador del pago a anular.
     * @param reason motivo de la anulación.
     * @return respuesta con el detalle del pago anulado.
     */
    @PostMapping("/{id}/void")
    public ResponseEntity<?> voidPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            PaymentDTO dto = paymentService.voidPayment(id, reason);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) { // no encontrado
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) { // ya anulado
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Opcional: obtiene un pago por su identificador.
     * Acceso: solo usuarios con rol ADMIN.
     * @param id identificador del pago.
     * @return ResponseEntity con el pago si existe, o 404 si no se encuentra.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> findById(@PathVariable Long id){
        Optional<PaymentDTO> payment = paymentService.findDTOById(id);
        return payment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Estado de un período (mes/año) calculado on-the-fly.
     * UP_TO_DATE si existe Payment válido para (client,month,year),
     * si no existe: PENDING si hoy <= dueDate+3, EXPIRED si hoy > dueDate+3.
     */
    @GetMapping("/state")
    public ResponseEntity<?> getPeriodState(
            @RequestParam Long clientId,
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        try {
            PaymentState state = paymentService.computePeriodState(clientId, month, year);
            LocalDate due = paymentService.computeDueDate(month, year);
            LocalDate graceEnd = due.plusDays(3);
            return ResponseEntity.ok(Map.of(
                    "state", state.name(),
                    "dueDate", due.toString(),
                    "graceEnd", graceEnd.toString()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
