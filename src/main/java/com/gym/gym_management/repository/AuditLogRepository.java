package com.gym.gym_management.repository;

import com.gym.gym_management.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Spring Data crea automáticamente el CRUD para AuditLog a partir de esta interfaz.
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Métodos personalizados
    List<AuditLog> findByEntityAndEntityId(String entity, Long entityId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
