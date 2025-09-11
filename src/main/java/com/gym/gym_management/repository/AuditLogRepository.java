package com.gym.gym_management.repository;

import com.gym.gym_management.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Métodos personalizados
    List<AuditLog> findByEntityAndEntityId(String entity, Long entityId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
