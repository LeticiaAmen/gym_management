package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.controller.dto.OverdueClientDTO;
import com.gym.gym_management.controller.dto.ExpiringClientDTO;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.repository.IClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    /**
     * Obtiene clientes con pagos por vencer en los próximos 7 días (no vencidos).
     * Devuelve además la fecha de expiración del último pago válido (no anulado).
     */
    public List<ExpiringClientDTO> getClientsWithPaymentsExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        return clientRepository.findAllActive().stream()
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
                    return dto;
                }
                return null;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }

    /**
     * Obtiene clientes activos cuya última membresía (último pago válido no anulado) ya venció.
     * Devuelve además la fecha de expiración de ese último pago.
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
        return candidates.stream()
            .map(c -> toOverdueDTO(c, paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(c.getId())))
            .filter(dto -> dto.getExpirationDate() != null && dto.getExpirationDate().isBefore(today))
            .collect(Collectors.toList());
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
        return dto;
    }
}
