package com.gym.gym_management.service;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private IPaymentRepository paymentRepository;
    private IClientRepository clientRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(IPaymentRepository.class);
        clientRepository = mock(IClientRepository.class);
        paymentService = new PaymentService();
        ReflectionTestUtils.setField(paymentService, "paymentRepository", paymentRepository);
        ReflectionTestUtils.setField(paymentService, "clientRepository", clientRepository);
    }

    @Test
    void getLatestPaymentUpToDate() {
        Payment payment = new Payment();
        payment.setExpirationDate(LocalDate.now().plusDays(5));
        payment.setPaymentState(PaymentState.UP_TO_DATE);
        when(paymentRepository.findTopByClientIdOrderByExpirationDateDesc(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getLatestPayment(1L);

        assertThat(result.getPaymentState()).isEqualTo(PaymentState.UP_TO_DATE);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getLatestPaymentExpired() {
        Payment payment = new Payment();
        payment.setExpirationDate(LocalDate.now().minusDays(1));
        payment.setPaymentState(PaymentState.UP_TO_DATE);
        when(paymentRepository.findTopByClientIdOrderByExpirationDateDesc(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getLatestPayment(1L);

        assertThat(result.getPaymentState()).isEqualTo(PaymentState.EXPIRED);
        verify(paymentRepository).save(payment);
    }
}
