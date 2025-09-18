package com.gym.gym_management.repository;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByClientId(Long clientId);

    // Usado por job de recordatorios (filtra además voided=false)
    List<Payment> findByExpirationDateAndPaymentStateAndVoidedFalse(LocalDate date, PaymentState paymentState);
    // Usado para obtener vencidos (estado ya materializado como EXPIRED)
    List<Payment> findByExpirationDateBeforeAndPaymentState(LocalDate date, PaymentState paymentState);

    List<Payment> findByPaymentDateBetweenAndVoidedFalse(LocalDate from, LocalDate to);

    // Filtros simples (estado persistido: UP_TO_DATE | EXPIRED | VOIDED)
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

    boolean existsByClient_IdAndMonthAndYearAndVoidedFalse(Long clientId, Integer month, Integer year);

    Payment findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(Long clientId);

    @Query("SELECT p FROM Payment p WHERE p.client.id = :clientId AND p.voided = false ORDER BY COALESCE(p.expirationDate, p.paymentDate) DESC")
    Payment findLastEffectiveByClient(@Param("clientId") Long clientId);

    long countByPaymentStateAndVoidedFalse(PaymentState paymentState);

    List<Payment> findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate date);

    List<Payment> findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT p FROM Payment p JOIN FETCH p.client c WHERE p.expirationDate = :date AND p.voided = false AND p.paymentState = 'UP_TO_DATE'")
    List<Payment> findByExpirationDateWithClient(@Param("date") LocalDate date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Payment p SET p.paymentState=:expired WHERE p.paymentState=:upToDate AND p.voided=false AND p.expirationDate < :today")
    int bulkExpire(@Param("today") LocalDate today,
                   @Param("upToDate") PaymentState upToDate,
                   @Param("expired") PaymentState expired);
}
