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

    @Autowired
    private ClientService clientService;

    // Listar o filtrar clientes
    @GetMapping
    public ResponseEntity<List<ClientDTO>> find(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "payment", required = false) PaymentState payment
    ) {
        if (q != null || active != null || payment != null) {
            return ResponseEntity.ok(clientService.search(q, active, payment));
        }
        return ResponseEntity.ok(clientService.findAll());
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
