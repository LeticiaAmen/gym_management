package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.repository.IClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    /**
     * Obtiene clientes con pagos por vencer en los próximos 7 días
     */
    public List<ClientDTO> getClientsWithPaymentsExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        return clientRepository.findAllActive().stream()
            .filter(client -> client.getPayments().stream()
                .anyMatch(payment ->
                    !payment.isVoided() &&
                    payment.getExpirationDate() != null &&
                    payment.getExpirationDate().isAfter(today) &&
                    payment.getExpirationDate().isBefore(sevenDaysFromNow)))
            .map(this::toClientDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtiene clientes con pagos vencidos
     */
    public List<ClientDTO> getClientsWithOverduePayments() {
        LocalDate today = LocalDate.now();

        return clientRepository.findAllActive().stream()
            .filter(client -> client.getPayments().stream()
                .anyMatch(payment ->
                    !payment.isVoided() &&
                    payment.getExpirationDate() != null &&
                    payment.getExpirationDate().isBefore(today)))
            .map(this::toClientDTO)
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
}
