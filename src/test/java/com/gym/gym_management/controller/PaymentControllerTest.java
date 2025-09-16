package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.model.PaymentMethod;
import com.gym.gym_management.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock private PaymentService paymentService;
    @InjectMocks private PaymentController controller;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PaymentDTO buildValidPayment() {
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
    void actualizaPago_invalidoDevuelve400ConDetalleDeErrores() throws Exception {
        PaymentDTO input = buildValidPayment();
        given(paymentService.registerPayment(any(PaymentDTO.class)))
                .willThrow(new IllegalArgumentException("El mes debe estar entre 1 y 12"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El mes debe estar entre 1 y 12"));
    }

    @Test
    void registraPago_duplicadoPeriodoDevuelve409() throws Exception {
        PaymentDTO input = buildValidPayment();
        given(paymentService.registerPayment(any(PaymentDTO.class)))
                .willThrow(new IllegalStateException("Ya existe un pago válido para ese período"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Ya existe un pago válido para ese período"));
    }

    @Test
    void obtienePagoInexistente_devuelve404() throws Exception {
        given(paymentService.findDTOById(999L)).willReturn(Optional.empty());
        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }
}
