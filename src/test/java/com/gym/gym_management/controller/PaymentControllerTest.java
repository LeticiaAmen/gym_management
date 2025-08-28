package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.controller.dto.RegisterPaymentRequest;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @WithMockUser(roles = "USER")
    void registerPayment() throws Exception {
        RegisterPaymentRequest request = new RegisterPaymentRequest();
        request.setAmount(100.0);
        request.setPaymentDate(LocalDate.of(2024, 1, 1));
        request.setDuration(RegisterPaymentRequest.Duration.QUINCE_DIAS);

        Payment payment = new Payment();
        payment.setAmount(100.0);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setExpirationDate(request.getPaymentDate().plusDays(15));

        when(paymentService.registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble()))
                .thenReturn(payment);

        mockMvc.perform(post("/payments/client/{clientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expirationDate").value(payment.getExpirationDate().toString()));

        verify(paymentService).registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble());
    }
}