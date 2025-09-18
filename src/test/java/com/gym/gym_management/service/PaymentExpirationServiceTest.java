package com.gym.gym_management.service;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentMethod;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración para verificar la materialización de estado EXPIRED mediante expireOverduePayments().
 * Escenarios validados:
 * 1) Pagos vencidos (expirationDate < hoy) y UP_TO_DATE pasan a EXPIRED.
 * 2) Pagos no vencidos permanecen UP_TO_DATE.
 * 3) Pagos voided no se modifican.
 * 4) Idempotencia: segunda ejecución no cambia conteos.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentExpirationServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setFirstName("Test");
        client.setLastName("Cliente");
        client.setEmail("test.expire@example.com");
        client.setActive(true);
        clientRepository.save(client);

        LocalDate hoy = LocalDate.now();

        // Vencido -> debe expirarse
        paymentRepository.save(buildPayment(hoy.minusDays(15), hoy.minusDays(1), PaymentState.UP_TO_DATE, false));
        // No vencido -> permanece al día
        paymentRepository.save(buildPayment(hoy.minusDays(5), hoy.plusDays(10), PaymentState.UP_TO_DATE, false));
        // Vencido pero voided -> no debe cambiar
        paymentRepository.save(buildPayment(hoy.minusDays(20), hoy.minusDays(2), PaymentState.UP_TO_DATE, true));
    }

    private Payment buildPayment(LocalDate paymentDate, LocalDate expirationDate, PaymentState state, boolean voided) {
        Payment p = new Payment();
        p.setClient(client);
        p.setAmount(1000.0);
        p.setMethod(PaymentMethod.CASH);
        p.setMonth(paymentDate.getMonthValue());
        p.setYear(paymentDate.getYear());
        p.setPaymentDate(paymentDate);
        p.setExpirationDate(expirationDate);
        p.setState(state);
        p.setVoided(voided);
        return p;
    }

    @Test
    @DisplayName("expireOverduePayments actualiza sólo los vencidos y es idempotente")
    void expireOverduePayments_updatesAndIsIdempotent() {
        int firstRun = paymentService.expireOverduePayments();
        assertThat(firstRun).isEqualTo(1); // sólo uno califica (vencido y no voided)

        // Verificar estados tras primera ejecución
        List<Payment> all = paymentRepository.findByClientId(client.getId());
        long expired = all.stream().filter(p -> p.getState() == PaymentState.EXPIRED).count();
        long upToDate = all.stream().filter(p -> p.getState() == PaymentState.UP_TO_DATE && !p.isVoided()).count();
        long voided = all.stream().filter(Payment::isVoided).count();

        assertThat(expired).isEqualTo(1);
        assertThat(upToDate).isEqualTo(1); // el que no está vencido
        assertThat(voided).isEqualTo(1); // permanece voided (estado original)

        // Segunda ejecución idempotente
        int secondRun = paymentService.expireOverduePayments();
        assertThat(secondRun).isEqualTo(0);
    }
}

