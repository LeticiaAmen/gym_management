package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ClientDTO;
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
 * - GET    /clients                 → Lista todos los clientes.
 * - POST   /clients                 → Crea un nuevo cliente (requiere rol USER).
 * - PUT    /clients/{id}            → Actualiza un cliente existente.
 * - PATCH  /clients/{id}/deactivate → Desactiva un cliente.
 * - PATCH  /clients/{id}/activate   → Activa un cliente.
 * - DELETE /clients/{id}            → Elimina (soft delete) un cliente por id.
 *
 * Relación con los requerimientos:
 * - "Gestión de Clientes (Administradores)": crear, modificar, eliminar clientes.
 * - "Autenticación y Seguridad": el alta de clientes está protegida por roles (solo USER).
 * - "Accesibilidad Web": expone endpoints REST para uso por web/móvil/Postman.
 */

@RestController
@RequestMapping("/api/clients")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    @Autowired
    private ClientService clientService;

    // Listar clientes (simple)
    @GetMapping
    public ResponseEntity<List<ClientDTO>> findAll() {
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
