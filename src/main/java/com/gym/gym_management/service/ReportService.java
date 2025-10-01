package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.controller.dto.OverdueClientDTO;
import com.gym.gym_management.controller.dto.ExpiringClientDTO;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.NotificationLog;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.INotificationLogRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.repository.IClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private INotificationLogRepository notificationLogRepository;

    /**
     * Obtiene clientes con pagos por vencer en los próximos 7 días (no vencidos).
     * Devuelve además la fecha de expiración del último pago válido (no anulado)
     * y si se les envió recordatorio.
     */
    public List<ExpiringClientDTO> getClientsWithPaymentsExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        // Obtener clientes próximos a vencer
        List<ExpiringClientDTO> expiringClients = clientRepository.findAllActive().stream()
            .map(client -> {
                Payment lastValid = client.getPayments().stream()
                    .filter(p -> !p.isVoided() && p.getExpirationDate() != null)
                    .max(Comparator.comparing(Payment::getExpirationDate))
                    .orElse(null);
                if (lastValid == null) return null;
                LocalDate exp = lastValid.getExpirationDate();
                boolean notExpired = !exp.isBefore(today); // exp >= today
                boolean within7 = exp.isBefore(sevenDaysFromNow) || exp.isEqual(sevenDaysFromNow);
                if (notExpired && within7) {
                    ExpiringClientDTO dto = new ExpiringClientDTO();
                    dto.setId(client.getId());
                    dto.setFirstName(client.getFirstName());
                    dto.setLastName(client.getLastName());
                    dto.setEmail(client.getEmail());
                    dto.setActive(client.isActive());
                    dto.setExpirationDate(exp);
                    dto.setReminderSent(false); // Se establecerá después
                    return dto;
                }
                return null;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());

        // Obtener información de notificaciones para estos clientes
        if (!expiringClients.isEmpty()) {
            List<Long> paymentIds = expiringClients.stream()
                .map(dto -> getLastValidPaymentId(dto.getId()))
                .filter(id -> id != null)
                .collect(Collectors.toList());

            if (!paymentIds.isEmpty()) {
                List<NotificationLog> sentNotifications = notificationLogRepository
                    .findSentNotificationsByPaymentIds(paymentIds);

                Map<Long, Boolean> notificationMap = sentNotifications.stream()
                    .collect(Collectors.toMap(
                        NotificationLog::getPaymentId,
                        nl -> true,
                        (existing, replacement) -> existing
                    ));

                // Actualizar DTOs con información de notificaciones
                expiringClients.forEach(dto -> {
                    Long paymentId = getLastValidPaymentId(dto.getId());
                    if (paymentId != null) {
                        dto.setReminderSent(notificationMap.getOrDefault(paymentId, false));
                    }
                });
            }
        }

        return expiringClients;
    }

    /**
     * Obtiene clientes activos cuya última membresía (último pago válido no anulado) ya venció.
     * Devuelve además la fecha de expiración de ese último pago y si se les envió recordatorio.
     */
    public List<OverdueClientDTO> getClientsWithOverduePayments() {
        LocalDate today = LocalDate.now();
        // Opción principal: una sola consulta evita N+1 para determinar candidatos
        List<Client> candidates = clientRepository.findActiveClientsWithLastPaymentExpired(today);
        if (candidates == null || candidates.isEmpty()) {
            // Fallback: calcular por cliente con el último pago válido
            candidates = clientRepository.findAllActive().stream()
                .filter(c -> {
                    Payment last = paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(c.getId());
                    return last != null && last.getExpirationDate() != null && last.getExpirationDate().isBefore(today);
                })
                .collect(Collectors.toList());
        }

        // Para cada cliente candidato obtenemos el último pago para exponer su expirationDate
        List<OverdueClientDTO> overdueClients = candidates.stream()
            .map(c -> {
                Payment lastPayment = paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(c.getId());
                return toOverdueDTO(c, lastPayment);
            })
            .filter(dto -> dto.getExpirationDate() != null && dto.getExpirationDate().isBefore(today))
            .collect(Collectors.toList());

        // Obtener información de notificaciones para estos clientes
        if (!overdueClients.isEmpty()) {
            List<Long> paymentIds = overdueClients.stream()
                .map(dto -> getLastValidPaymentId(dto.getId()))
                .filter(id -> id != null)
                .collect(Collectors.toList());

            if (!paymentIds.isEmpty()) {
                List<NotificationLog> sentNotifications = notificationLogRepository
                    .findSentNotificationsByPaymentIds(paymentIds);

                Map<Long, Boolean> notificationMap = sentNotifications.stream()
                    .collect(Collectors.toMap(
                        NotificationLog::getPaymentId,
                        nl -> true,
                        (existing, replacement) -> existing
                    ));

                // Actualizar DTOs con información de notificaciones
                overdueClients.forEach(dto -> {
                    Long paymentId = getLastValidPaymentId(dto.getId());
                    if (paymentId != null) {
                        dto.setReminderSent(notificationMap.getOrDefault(paymentId, false));
                    }
                });
            }
        }

        return overdueClients;
    }

    /**
     * Obtiene el ID del último pago válido de un cliente.
     *
     * @param clientId ID del cliente
     * @return ID del último pago válido o null si no existe
     */
    private Long getLastValidPaymentId(Long clientId) {
        Payment lastPayment = paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(clientId);
        return lastPayment != null ? lastPayment.getId() : null;
    }

    /**
     * Calcula el flujo de caja entre dos fechas
     */
    public Double calculateCashflow(LocalDate from, LocalDate to) {
        return paymentRepository.findByPaymentDateBetweenAndVoidedFalse(from, to).stream()
            .mapToDouble(Payment::getAmount)
            .sum();
    }

    private ClientDTO toClientDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setFirstName(client.getFirstName());
        dto.setLastName(client.getLastName());
        dto.setEmail(client.getEmail());
        dto.setActive(client.isActive());
        return dto;
    }

    private OverdueClientDTO toOverdueDTO(Client client, Payment last) {
        OverdueClientDTO dto = new OverdueClientDTO();
        dto.setId(client.getId());
        dto.setFirstName(client.getFirstName());
        dto.setLastName(client.getLastName());
        dto.setEmail(client.getEmail());
        dto.setActive(client.isActive());
        dto.setExpirationDate(last != null ? last.getExpirationDate() : null);
        dto.setReminderSent(false); // Se establecerá después en el método principal
        return dto;
    }
}
