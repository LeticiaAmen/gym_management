package com.gym.gym_management.service;

import com.gym.gym_management.model.AuditLog;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Auditoría de clientes
    public void logClientCreation(Client client) {
        createLog("CREATE_CLIENT", "Client", client.getId(), null, client.toString());
    }

    public void logClientUpdate(Client oldClient, Client newClient) {
        createLog("UPDATE_CLIENT", "Client", newClient.getId(), oldClient.toString(), newClient.toString());
    }

    public void logClientDeactivation(Client client) {
        createLog("DEACTIVATE_CLIENT", "Client", client.getId(), "active=true", "active=false");
    }

    public void logClientPause(Client client) {
        createLog("PAUSE_CLIENT", "Client", client.getId(), null,
                String.format("pausado desde %s hasta %s: %s",
                        client.getPausedFrom(), client.getPausedTo(), client.getPauseReason()));
    }

    public void logClientResume(Client client) {
        createLog("RESUME_CLIENT", "Client", client.getId(), null, "suscripción reactivada");
    }

    // Auditoría de pagos
    public void logPaymentCreation(Payment payment) {
        createLog("CREATE_PAYMENT", "Payment", payment.getId(), null,
                String.format("monto: $%.2f, método: %s", payment.getAmount(), payment.getMethod()));
    }

    public void logPaymentVoid(Payment payment, String reason) {
        createLog("VOID_PAYMENT", "Payment", payment.getId(), "activo",
                String.format("anulado: %s", reason));
    }

    private void createLog(String action, String entity, Long entityId, String oldValues, String newValues) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(log);
    }
}
