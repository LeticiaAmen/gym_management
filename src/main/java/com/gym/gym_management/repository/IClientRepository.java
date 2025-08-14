package com.gym.gym_management.repository;

import com.gym.gym_management.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad Client.
 *
 * Extiende JpaRepository, lo que proporciona métodos CRUD estándar:
 * - findAll(), findById(), save(), deleteById(), etc.
 *
 * Métodos personalizados:
 * - findByUserEmail(String email):
 *   Busca un cliente por el email asociado a su usuario (relación 1:1 con User).
 *
 * Relación con los requerimientos:
 * - "Gestión de Clientes": este repositorio permite recuperar y manipular
 *   la información de los clientes en la base de datos.
 * - Soporta las operaciones usadas en ClientService para crear, actualizar, eliminar y buscar clientes.
 */
public interface IClientRepository extends JpaRepository<Client, Long> {
    // busca un cliente por el mail de su usuario asociado.
    // return el cliente encontrado o null si no existe.
    Client findByUserEmail(String email);
}
