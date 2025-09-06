package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ClientUpdateRequestDTO;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/clients") //Ruta base para recursos de clientes
public class ClientController {

    // Servicio de dominio que encapsula la lógica de negocio de clientes
    @Autowired
    private ClientService clientService;

    /**
     * Obtiene la lista completa de clientes.
     * Acceso: autenticado (según SecurityConfiguration).
     * @return lista de entidades Client.
     */
    @GetMapping
    public List<Client> findAll(){
        return clientService.findAll();
    }


    /**
     * Crea un nuevo cliente.
     * Acceso: restringido a usuarios con rol "USER" (administrador en este sistema).
     *
     * @param client cuerpo JSON con los datos del cliente a crear.
     * @return el cliente creado (tal como fue persistido).
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Client save(@RequestBody Client client){
        return clientService.saveClient(client);
    }

    /**
     * Actualiza los datos de perfil (nombre, apellido y teléfono) de un cliente existente.
     * Acceso: restringido a usuarios con rol "USER" (administrador en este sistema).
     *
     * @param id identificador del cliente a modificar.
     * @param request datos de perfil a actualizar.
     *@return el cliente actualizado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Client update(@PathVariable Long id, @RequestBody ClientUpdateRequestDTO request){
        return clientService.updateProfile(id, request);
    }

    /**
     * Desactiva un cliente existente.
     * Acceso: restringido a usuarios con rol "USER".
     *
     * @param id identificador del cliente a desactivar.
     * @return el cliente actualizado con isActive = false.
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('USER')")
    public Client deactivate(@PathVariable Long id){
        return clientService.deactivateClient(id);
    }

    /**
     * Activa un cliente existente de manera idempotente.
     * Acceso: restringido a usuarios con rol "USER".
     *
     * @param id identificador del cliente a activar.
     * @return el cliente actualizado con isActive = true.
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('USER')")
    public Client activate(@PathVariable Long id){
        return clientService.activateClient(id);
    }

    /**
     * Elimina un cliente por su identificador.(soft-delete)
     * Acceso: restringido a usuarios con rol "USER" (administrador en este sistema).
     *
     * @param id identificador del cliente a eliminar.
     * @return respuesta 200 OK con un mensaje de confirmación.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> delete(@PathVariable Long id){
        clientService.deleteClient(id);
        return ResponseEntity.ok("Se eliminó el cliente con id: " + id);
    }

}
