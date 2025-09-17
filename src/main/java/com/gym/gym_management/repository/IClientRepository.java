package com.gym.gym_management.repository;

import com.gym.gym_management.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface IClientRepository extends JpaRepository<Client, Long> {

    @Query("SELECT c FROM Client c WHERE c.isActive = true")
    List<Client> findAllActive();

    @Query("SELECT c FROM Client c WHERE c.email = ?1")
    Client findByEmail(String email);

    boolean existsByEmail(String email);

    // para validar duplicados excluyendo el propio registro
    boolean existsByEmailAndIdNot(String email, Long id);

    // búsqueda por texto (nombre, apellido o email) y activo opcional
    @Query("SELECT c FROM Client c WHERE (:q IS NULL OR lower(c.firstName) LIKE lower(concat('%',:q,'%')) " +
            "OR lower(c.lastName) LIKE lower(concat('%',:q,'%')) " +
            "OR lower(c.email) LIKE lower(concat('%',:q,'%'))) " +
            "AND (:active IS NULL OR c.isActive = :active)")
    List<Client> search(@Param("q") String q, @Param("active") Boolean active);

    // Método para contar clientes activos (para dashboard)
    long countByIsActiveTrue();

    // Método para obtener clientes registrados recientemente
    List<Client> findByStartDateAfterOrderByStartDateDesc(LocalDate date);

    // Clientes activos cuyo último pago válido (no anulado) está vencido
    @Query("SELECT c FROM Client c WHERE c.isActive = true AND EXISTS (" +
           " SELECT 1 FROM Payment p WHERE p.client = c AND p.voided = false AND p.expirationDate = (" +
           "   SELECT MAX(p2.expirationDate) FROM Payment p2 WHERE p2.client = c AND p2.voided = false" +
           " ) AND p.expirationDate < :today" +
           ")")
    List<Client> findActiveClientsWithLastPaymentExpired(@Param("today") LocalDate today);

    // Clientes activos SIN ningún pago válido vigente (no anulado) a partir de hoy
    @Query("SELECT c FROM Client c WHERE c.isActive = true AND NOT EXISTS (" +
           " SELECT 1 FROM Payment p WHERE p.client = c AND p.voided = false AND p.expirationDate >= :today" +
           ")")
    List<Client> findActiveClientsWithoutValidPayment(@Param("today") LocalDate today);
}
