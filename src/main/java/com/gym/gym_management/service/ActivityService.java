package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ActivityDto;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private IPaymentRepository paymentRepository;

    public List<ActivityDto> getRecentActivities(int limit) {
        List<ActivityDto> activities = new ArrayList<>();

        // Obtener clientes recientes (últimos 7 días)
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Client> recentClients = clientRepository.findByStartDateAfterOrderByStartDateDesc(sevenDaysAgo);

        for (Client client : recentClients.stream().limit(5).collect(Collectors.toList())) {
            String clientName = String.format("%s %s",
                client.getFirstName() != null ? client.getFirstName() : "",
                client.getLastName() != null ? client.getLastName() : "").trim();

            activities.add(new ActivityDto(
                "new-client",
                String.format("Nuevo cliente registrado: %s", clientName),
                String.format("Cliente registrado con email: %s", client.getEmail()),
                client.getStartDate().atStartOfDay(),
                client.getId()
            ));
        }

        // Obtener pagos recientes (últimos 7 días)
        LocalDate paymentSevenDaysAgo = LocalDate.now().minusDays(7);
        List<Payment> recentPayments = paymentRepository.findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(paymentSevenDaysAgo);

        for (Payment payment : recentPayments.stream().limit(5).collect(Collectors.toList())) {
            Client client = payment.getClient();
            String clientName = String.format("%s %s",
                client.getFirstName() != null ? client.getFirstName() : "",
                client.getLastName() != null ? client.getLastName() : "").trim();

            activities.add(new ActivityDto(
                "payment",
                String.format("Pago recibido: $%.2f de %s", payment.getAmount(), clientName),
                String.format("Pago por método: %s", payment.getMethod().toString()),
                payment.getPaymentDate().atStartOfDay(),
                payment.getId()
            ));
        }

        // Obtener clientes con membresías próximas a vencer (próximos 3 días)
        LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
        List<Payment> expiringPayments = paymentRepository.findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(
            LocalDate.now(), threeDaysFromNow);

        for (Payment payment : expiringPayments.stream().limit(3).collect(Collectors.toList())) {
            Client client = payment.getClient();
            String clientName = String.format("%s %s",
                client.getFirstName() != null ? client.getFirstName() : "",
                client.getLastName() != null ? client.getLastName() : "").trim();

            activities.add(new ActivityDto(
                "expiring",
                String.format("Membresía por vencer: %s", clientName),
                String.format("Vence el %s", payment.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                payment.getExpirationDate().atStartOfDay(),
                client.getId()
            ));
        }

        // Ordenar por fecha descendente y limitar
        return activities.stream()
            .sorted(Comparator.comparing(ActivityDto::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
