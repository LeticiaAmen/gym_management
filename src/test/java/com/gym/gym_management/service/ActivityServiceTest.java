package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ActivityDto;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentMethod;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ActivityServiceTest {

    @Mock private IClientRepository clientRepository;
    @Mock private IPaymentRepository paymentRepository;

    @InjectMocks private ActivityService activityService;

    private Client activeClient1;
    private Client activeClient2;

    @BeforeEach
    void setUp() {
        activeClient1 = new Client();
        activeClient1.setId(1L);
        activeClient1.setFirstName("Ana");
        activeClient1.setLastName("García");
        activeClient1.setEmail("ana@example.com");
        activeClient1.setStartDate(LocalDate.now().minusDays(2));

        activeClient2 = new Client();
        activeClient2.setId(2L);
        activeClient2.setFirstName("Juan");
        activeClient2.setLastName("Pérez");
        activeClient2.setEmail("juan@example.com");
        activeClient2.setStartDate(LocalDate.now().minusDays(5));
    }

    @Test
    void getRecentActivities_returnsCombinedSortedAndLimitedActivities() {
        // recent clients within last 7 days
        given(clientRepository.findByStartDateAfterOrderByStartDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of(activeClient1, activeClient2));

        // recent payments within last 7 days
        Payment pay1 = new Payment();
        pay1.setId(10L);
        pay1.setAmount(5000.0);
        pay1.setMethod(PaymentMethod.CASH);
        pay1.setPaymentDate(LocalDate.now().minusDays(1));
        pay1.setClient(activeClient1);

        Payment pay2 = new Payment();
        pay2.setId(11L);
        pay2.setAmount(6000.0);
        pay2.setMethod(PaymentMethod.CREDIT);
        pay2.setPaymentDate(LocalDate.now().minusDays(4));
        pay2.setClient(activeClient2);

        given(paymentRepository.findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of(pay1, pay2));

        // expiring payments between now and 3 days from now
        Payment expiring = new Payment();
        expiring.setId(12L);
        expiring.setAmount(7000.0);
        expiring.setMethod(PaymentMethod.DEBIT);
        expiring.setExpirationDate(LocalDate.now().plusDays(2));
        expiring.setClient(activeClient1);
        given(paymentRepository.findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate.now(), LocalDate.now().plusDays(3)))
                .willReturn(List.of(expiring));

        List<ActivityDto> list = activityService.getRecentActivities(5);

        assertThat(list).isNotEmpty();
        // limited to requested size and sorted by timestamp desc
        assertThat(list.size()).isLessThanOrEqualTo(5);
        assertThat(list).isSortedAccordingTo((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        // contains at least one of each type
        assertThat(list).extracting(ActivityDto::getType).contains("new-client", "payment", "expiring");
    }

    @Test
    void getRecentActivities_handlesEmptySourcesReturningEmptyList() {
        given(clientRepository.findByStartDateAfterOrderByStartDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of());
        given(paymentRepository.findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of());
        given(paymentRepository.findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate.now(), LocalDate.now().plusDays(3)))
                .willReturn(List.of());

        List<ActivityDto> out = activityService.getRecentActivities(10);
        assertThat(out).isEmpty();
    }

    @Test
    void getRecentActivities_trimsNullNamesGracefully() {
        Client c = new Client();
        c.setId(3L);
        c.setFirstName(null);
        c.setLastName(null);
        c.setEmail("nulls@example.com");
        c.setStartDate(LocalDate.now());
        given(clientRepository.findByStartDateAfterOrderByStartDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of(c));
        given(paymentRepository.findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of());
        given(paymentRepository.findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate.now(), LocalDate.now().plusDays(3)))
                .willReturn(List.of());

        List<ActivityDto> out = activityService.getRecentActivities(5);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getType()).isEqualTo("new-client");
        assertThat(out.get(0).getRelatedId()).isEqualTo(3L);
    }

    @Test
    void getRecentActivities_includesOnlyExpiringWithinThreeDays() {
        Payment inside = new Payment();
        inside.setId(20L);
        inside.setExpirationDate(LocalDate.now().plusDays(1));
        inside.setClient(activeClient1);

        given(clientRepository.findByStartDateAfterOrderByStartDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of());
        given(paymentRepository.findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate.now().minusDays(7)))
                .willReturn(List.of());
        given(paymentRepository.findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate.now(), LocalDate.now().plusDays(3)))
                .willReturn(List.of(inside));

        List<ActivityDto> out = activityService.getRecentActivities(10);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getRelatedId()).isEqualTo(activeClient1.getId());
        assertThat(out.get(0).getType()).isEqualTo("expiring");
    }
}
