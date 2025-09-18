package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador REST para gestionar operaciones sobre clientes.
 *
 * Endpoints:
 * - GET    /clients                 → Lista todos los clientes o filtra por q/active/payment.
 * - POST   /clients                 → Crea un nuevo cliente.
 * - PUT    /clients/{id}            → Actualiza un cliente existente.
 * - PATCH  /clients/{id}/deactivate → Desactiva un cliente.
 * - PATCH  /clients/{id}/activate   → Activa un cliente.
 * - DELETE /clients/{id}            → Elimina (soft delete) un cliente por id.
 */

@RestController
@RequestMapping("/api/clients")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private ClientService clientService;

    // Listar o filtrar clientes
    @GetMapping
    public ResponseEntity<List<ClientDTO>> find(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "active", required = false) String activeParam,
            @RequestParam(value = "payment", required = false) String paymentParam
    ) {
        log.debug("[GET /clients] q={}, activeParam='{}', paymentParam='{}'", q, activeParam, paymentParam);
        Boolean active = null;
        if (activeParam != null && !activeParam.isBlank()) {
            String v = activeParam.trim().toLowerCase();
            if (v.equals("true") || v.equals("active") || v.equals("activos")) {
                active = true;
            } else if (v.equals("false") || v.equals("inactive") || v.equals("inactivos")) {
                active = false;
            } else if (v.equals("all") || v.equals("todos")) {
                active = null; // sin filtro
            } else {
                log.debug("[GET /clients] valor 'active' desconocido '{}', se ignora", v);
            }
        }
        PaymentState paymentState = null;
        if (paymentParam != null && !paymentParam.isBlank()) {
            String v = paymentParam.trim().toUpperCase();
            if (v.equals("ALL") || v.equals("TODOS")) {
                paymentState = null;
            } else {
                try {
                    paymentState = PaymentState.valueOf(v);
                } catch (IllegalArgumentException ex) {
                    log.debug("[GET /clients] valor 'payment' desconocido '{}', se ignora", v);
                }
            }
        }
        if (q != null || active != null || paymentState != null) {
            List<ClientDTO> out = clientService.search(q, active, paymentState);
            log.debug("[GET /clients] filtros aplicados -> {} resultados", out.size());
            return ResponseEntity.ok(out);
        }
        List<ClientDTO> all = clientService.findAll();
        log.debug("[GET /clients] sin filtros -> {} resultados", all.size());
        return ResponseEntity.ok(all);
    }

    // Crear cliente
    @PostMapping
    public ResponseEntity<ClientDTO> create(@Valid @RequestBody ClientDTO clientDTO) {
        ClientDTO created = clientService.create(clientDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Actualizar cliente
    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> update(@PathVariable Long id, @Valid @RequestBody ClientDTO clientDTO) {
        ClientDTO updated = clientService.update(id, clientDTO);
        return ResponseEntity.ok(updated);
    }

    // Baja lógica (inactivar)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // Activar nuevamente
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        clientService.activate(id);
        return ResponseEntity.noContent().build();
    }

    // Pausar suscripción
    @PostMapping("/{id}/pause")
    public ResponseEntity<ClientDTO> pause(
            @PathVariable Long id,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "reason", required = false) String reason) {
        ClientDTO paused = clientService.pause(id, from, to, reason);
        return ResponseEntity.ok(paused);
    }

    // Reanudar suscripción
    @PostMapping("/{id}/resume")
    public ResponseEntity<ClientDTO> resume(@PathVariable Long id) {
        ClientDTO resumed = clientService.resume(id);
        return ResponseEntity.ok(resumed);
    }
}
