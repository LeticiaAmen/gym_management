package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.controller.dto.RegisterPaymentRequest;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        payment.setId(1L);
        payment.setAmount(100.0);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setExpirationDate(request.getPaymentDate().plusDays(15));
        payment.setPaymentState(PaymentState.UP_TO_DATE);
        Client client = new Client();
        ReflectionTestUtils.setField(client, "id", 1L);
        payment.setClient(client);

        when(paymentService.registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble()))
                .thenReturn(payment);

        mockMvc.perform(post("/payments/client/{clientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.paymentDate").value(payment.getPaymentDate().toString()))
                .andExpect(jsonPath("$.expirationDate").value(payment.getExpirationDate().toString()))
                .andExpect(jsonPath("$.amount").value(payment.getAmount()))
                .andExpect(jsonPath("$.paymentState").value(payment.getPaymentState().toString()))
                .andExpect(jsonPath("$.clientId").value(1L))
                .andExpect(jsonPath("$.client").doesNotExist());

        verify(paymentService).registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble());
    }

    @Test
    @WithMockUser(roles = "USER")
    void registerPaymentWithNullDuration() throws Exception {
        RegisterPaymentRequest request = new RegisterPaymentRequest();
        request.setAmount(100.0); //monto del pago
        request.setPaymentDate(LocalDate.of(2024, 1, 1)); //fecha del pago fija
        // duration se deja en null para simular la solicitud inválida

        //se realiza la llamada al endpoint esperando un 400 bad request
        mockMvc.perform(post("/payments/client/{clientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // el servicio no debe ser invocado cuando la duración es nula.
        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void registerPaymentWithInactiveClient() throws Exception {
        RegisterPaymentRequest request = new RegisterPaymentRequest();
        request.setAmount(1000.0);
        request.setPaymentDate(LocalDate.of(2024, 1, 1));
        request.setDuration(RegisterPaymentRequest.Duration.QUINCE_DIAS);

        when(paymentService.registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble()))
                .thenThrow(new IllegalStateException("El cliente está inactivo"));

        mockMvc.perform(post("/payments/client/{clientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(paymentService).registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble());
    }
}