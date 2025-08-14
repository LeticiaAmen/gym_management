package com.gym.gym_management.repository;

import com.gym.gym_management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Payment.
 *
 * Extiende JpaRepository, lo que proporciona métodos CRUD estándar:
 * - findAll(), findById(), save(), deleteById(), etc.
 *
 * Métodos personalizados:
 * - findByClientId(Long clientId):
 *   Devuelve todos los pagos asociados a un cliente específico.
 *
 * Relación con los requerimientos:
 * - "Historial de Pagos": permite obtener todos los pagos de un cliente
 *   para que pueda consultar su historial o verificar si tiene pagos vencidos.
 * - "Panel de Administrador": facilita la gestión y seguimiento de pagos
 *   vinculados a cada cliente.
 */
public interface IPaymentRepository extends JpaRepository<Payment, Long> {
    // Busca todos los pagos asociados a un cliente mediante su ID.
    //@return lista de pagos realizados por el cliente, vacía si no tiene pagos.
    List<Payment> findByClientId(Long clientId);
}
