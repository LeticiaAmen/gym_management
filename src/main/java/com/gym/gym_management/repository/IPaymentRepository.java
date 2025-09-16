package com.gym.gym_management.repository;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByClientId(Long clientId);

    List<Payment> findByExpirationDateAndPaymentState(LocalDate date, PaymentState paymentState);

    List<Payment> findByExpirationDateBeforeAndPaymentState(LocalDate date, PaymentState paymentState);

    List<Payment> findByPaymentDateBetweenAndVoidedFalse(LocalDate from, LocalDate to);

    // Búsqueda con filtros opcionales + paginación
    @Query("SELECT p FROM Payment p WHERE (:clientId IS NULL OR p.client.id = :clientId) " +
           "AND (:from IS NULL OR p.paymentDate >= :from) " +
           "AND (:to IS NULL OR p.paymentDate <= :to) " +
           "AND (:state IS NULL OR p.paymentState = :state)")
    Page<Payment> findByFilters(
            @Param("clientId") Long clientId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("state") PaymentState state,
            Pageable pageable
    );

    // Idempotencia por período (ignorando pagos anulados)
    boolean existsByClient_IdAndMonthAndYearAndVoidedFalse(Long clientId, Integer month, Integer year);

    // Último pago válido (no anulado) para un cliente
    Payment findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(Long clientId);

    // Último pago efectivo: prioridad por expirationDate y si es null usa paymentDate
    @Query("SELECT p FROM Payment p WHERE p.client.id = :clientId AND p.voided = false ORDER BY COALESCE(p.expirationDate, p.paymentDate) DESC")
    Payment findLastEffectiveByClient(@Param("clientId") Long clientId);

    // Método para contar pagos expirados (para dashboard) - corregido el nombre del campo
    long countByExpirationDateBeforeAndPaymentStateAndVoidedFalse(LocalDate date, PaymentState paymentState);

    // Métodos para actividades recientes
    List<Payment> findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate date);

    List<Payment> findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate startDate, LocalDate endDate);
}
