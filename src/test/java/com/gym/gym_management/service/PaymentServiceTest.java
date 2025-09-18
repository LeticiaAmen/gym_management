package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.model.*;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private IPaymentRepository paymentRepository;
    @Mock private IClientRepository clientRepository;
    @Mock private IUserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks private PaymentService paymentService;

    private Client activeClient;

    @BeforeEach
    void init() {
        activeClient = new Client();
        activeClient.setId(1L);
        activeClient.setFirstName("Ana");
        activeClient.setLastName("García");
        activeClient.setEmail("ana@example.com");
        activeClient.setActive(true);
    }

    private PaymentDTO baseDto() {
        PaymentDTO dto = new PaymentDTO();
        dto.setClientId(1L);
        dto.setAmount(5000.0);
        dto.setMethod(PaymentMethod.CASH);
        dto.setMonth(LocalDate.now().getMonthValue());
        dto.setYear(LocalDate.now().getYear());
        dto.setPaymentDate(LocalDate.now().minusDays(1));
        return dto;
    }

    @Test
    void registerPayment_success_monthly() {
        PaymentDTO dto = baseDto();
        given(clientRepository.findById(1L)).willReturn(Optional.of(activeClient));
        given(paymentRepository.existsByClient_IdAndMonthAndYearAndVoidedFalse(1L, dto.getMonth(), dto.getYear())).willReturn(false);
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PaymentDTO out = paymentService.registerPayment(dto);
        assertThat(out.getId()).isEqualTo(10L);
        assertThat(out.getState()).isEqualTo(PaymentState.UP_TO_DATE);
        assertThat(out.getExpirationDate()).isEqualTo(dto.getPaymentDate().plusMonths(1));
        verify(auditService).logPaymentCreation(any(Payment.class));
    }

    @Test
    void registerPayment_duplicatePeriod_throws() {
        PaymentDTO dto = baseDto();
        given(clientRepository.findById(1L)).willReturn(Optional.of(activeClient));
        given(paymentRepository.existsByClient_IdAndMonthAndYearAndVoidedFalse(1L, dto.getMonth(), dto.getYear())).willReturn(true);
        assertThatThrownBy(() -> paymentService.registerPayment(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un pago válido");
    }

    @Test
    void voidPayment_marksVoided_andAudits_withAdminId() {
        Payment payment = new Payment();
        payment.setId(5L);
        payment.setAmount(1000.0);
        payment.setState(PaymentState.UP_TO_DATE);
        payment.setClient(activeClient);
        given(paymentRepository.findById(5L)).willReturn(Optional.of(payment));
        given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(User.builder().id(99L).email("admin@example.com").build()));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // mock security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@example.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        PaymentDTO out = paymentService.voidPayment(5L, "duplicado");
        assertThat(out.isVoided()).isTrue();
        assertThat(out.getState()).isEqualTo(PaymentState.VOIDED);
        assertThat(out.getVoidedBy()).isEqualTo(99L);
        verify(auditService).logPaymentVoid(any(Payment.class), eq("duplicado"));
        // cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void computeDueDate_validatesAndCalculates() {
        LocalDate due = paymentService.computeDueDate(2, 2025);
        assertThat(due.getYear()).isEqualTo(2025);
        assertThat(due.getMonthValue()).isEqualTo(2);
        assertThat(due.getDayOfMonth()).isEqualTo(10);
        assertThatThrownBy(() -> paymentService.computeDueDate(0, 2025)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> paymentService.computeDueDate(13, 1999)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computePeriodState_futurePeriodWithoutPayment_isUpToDate() {
        LocalDate today = LocalDate.now();
        int nextMonth = today.getMonthValue() == 12 ? 1 : today.getMonthValue() + 1;
        int year = today.getMonthValue() == 12 ? today.getYear() + 1 : today.getYear();

        given(clientRepository.existsById(1L)).willReturn(true);
        given(paymentRepository.findByClient_IdAndMonthAndYearAndVoidedFalse(1L, nextMonth, year)).willReturn(Optional.empty());

        PaymentState st = paymentService.computePeriodState(1L, nextMonth, year);
        assertThat(st).isEqualTo(PaymentState.UP_TO_DATE);
    }

    @Test
    void computePeriodState_pastPeriodWithoutPayment_isExpired() {
        LocalDate today = LocalDate.now();
        int prevMonth = today.getMonthValue() == 1 ? 12 : today.getMonthValue() - 1;
        int year = prevMonth == 12 && today.getMonthValue() == 1 ? today.getYear() - 1 : today.getYear();
        given(clientRepository.existsById(1L)).willReturn(true);
        given(paymentRepository.findByClient_IdAndMonthAndYearAndVoidedFalse(1L, prevMonth, year)).willReturn(Optional.empty());

        PaymentState st = paymentService.computePeriodState(1L, prevMonth, year);
        assertThat(st).isEqualTo(PaymentState.EXPIRED);
    }

    @Test
    void computePeriodState_returnsPersistedStateWhenPaymentExists() {
        LocalDate today = LocalDate.now();
        Payment payment = new Payment();
        payment.setState(PaymentState.EXPIRED); // simulamos que el job ya lo materializó
        given(clientRepository.existsById(1L)).willReturn(true);
        given(paymentRepository.findByClient_IdAndMonthAndYearAndVoidedFalse(1L, today.getMonthValue(), today.getYear())).willReturn(Optional.of(payment));

        PaymentState st = paymentService.computePeriodState(1L, today.getMonthValue(), today.getYear());
        assertThat(st).isEqualTo(PaymentState.EXPIRED);
    }
}
