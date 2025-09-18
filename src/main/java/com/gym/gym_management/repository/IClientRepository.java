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

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT c FROM Client c WHERE ( :q IS NULL OR (" +
            "LOWER(c.firstName) LIKE CONCAT('%', LOWER(:q), '%') OR " +
            "LOWER(c.lastName)  LIKE CONCAT('%', LOWER(:q), '%') OR " +
            "LOWER(c.email)     LIKE CONCAT('%', LOWER(:q), '%') ) ) " +
            "AND ( :active IS NULL OR c.isActive = :active )")
    List<Client> search(@Param("q") String q, @Param("active") Boolean active);

    @Query("SELECT c FROM Client c WHERE c.isActive = :active")
    List<Client> findByActive(@Param("active") boolean active);

    long countByIsActiveTrue();

    List<Client> findByStartDateAfterOrderByStartDateDesc(LocalDate date);

    @Query("SELECT c FROM Client c WHERE c.isActive = true AND EXISTS (" +
           " SELECT 1 FROM Payment p WHERE p.client = c AND p.voided = false AND p.expirationDate = (" +
           "   SELECT MAX(p2.expirationDate) FROM Payment p2 WHERE p2.client = c AND p2.voided = false" +
           " ) AND p.expirationDate < :today" +
           ")")
    List<Client> findActiveClientsWithLastPaymentExpired(@Param("today") LocalDate today);

    @Query("SELECT c FROM Client c WHERE c.isActive = true AND NOT EXISTS (" +
           " SELECT 1 FROM Payment p WHERE p.client = c AND p.voided = false AND p.expirationDate >= :today" +
           ")")
    List<Client> findActiveClientsWithoutValidPayment(@Param("today") LocalDate today);
}
