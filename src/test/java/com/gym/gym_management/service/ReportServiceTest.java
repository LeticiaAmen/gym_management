package com.gym.gym_management.service;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private IPaymentRepository paymentRepository;
    @Mock private IClientRepository clientRepository;

    @InjectMocks private ReportService reportService;

    @Test
    void getClientsWithPaymentsExpiringSoon_filtersWithin7Days() {
        Client a = new Client("Ana", "García", "ana@example.com", null);
        a.setId(1L);
        Client b = new Client("Juan", "Pérez", "juan@example.com", null);
        b.setId(2L);

        Payment pa = new Payment();
        pa.setExpirationDate(LocalDate.now().plusDays(3));
        pa.setVoided(false);
        a.addPayment(pa);

        Payment pb = new Payment();
        pb.setExpirationDate(LocalDate.now().plusDays(10)); // fuera de ventana
        pb.setVoided(false);
        b.addPayment(pb);

        given(clientRepository.findAllActive()).willReturn(List.of(a, b));

        var result = reportService.getClientsWithPaymentsExpiringSoon();
        assertThat(result).extracting("id").containsExactly(1L);
    }

    @Test
    void getClientsWithOverduePayments_filtersBeforeToday() {
        Client a = new Client("Ana", "García", "ana@example.com", null);
        a.setId(1L);
        Client b = new Client("Juan", "Pérez", "juan@example.com", null);
        b.setId(2L);

        Payment lastA = new Payment();
        lastA.setExpirationDate(LocalDate.now().minusDays(1)); // vencido
        lastA.setVoided(false);

        Payment lastB = new Payment();
        lastB.setExpirationDate(LocalDate.now().plusDays(1)); // no vencido
        lastB.setVoided(false);

        given(clientRepository.findAllActive()).willReturn(List.of(a, b));
        given(paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(1L)).willReturn(lastA);
        given(paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(2L)).willReturn(lastB);

        var result = reportService.getClientsWithOverduePayments();
        assertThat(result).extracting("id").containsExactly(1L);
    }

    @Test
    void calculateCashflow_sumsNonVoidedWithinRange() {
        var from = LocalDate.now().minusDays(30);
        var to = LocalDate.now();
        Payment p1 = new Payment(); p1.setAmount(100.0); p1.setVoided(false);
        Payment p2 = new Payment(); p2.setAmount(50.5); p2.setVoided(false);
        given(paymentRepository.findByPaymentDateBetweenAndVoidedFalse(from, to)).willReturn(List.of(p1, p2));
        Double total = reportService.calculateCashflow(from, to);
        assertThat(total).isEqualTo(150.5);
    }
}
