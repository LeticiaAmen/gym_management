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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @WithMockUser
    void getPaymentByClientId() throws Exception {
        // Simula que el test se ejecuta con un usuario autenticado (sin especificar roles).
        // Esto permite acceder al endpoint protegido por seguridad.

        // ---------- Objeto de prueba ----------
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setAmount(1000.0);
        payment.setPaymentDate(LocalDate.of(2024, 1, 1)); // Fecha de pago
        payment.setExpirationDate(LocalDate.of(2024, 1, 16)); // Vence a los 15 días
        payment.setPaymentState(PaymentState.UP_TO_DATE); // Estado esperado: al día

        // Se asocia un cliente al pago, con id = 1
        Client client = new Client();
        ReflectionTestUtils.setField(client, "id", 1L); // Se setea el id manualmente
        payment.setClient(client);

        // ---------- Configuración del mock ----------
        // Se indica que cuando se invoque a paymentService.getPaymentsByClientId(1),
        // se devuelva una lista con el objeto "payment".
        when(paymentService.getPaymentsByClientId(1L)).thenReturn(List.of(payment));

        // ---------- Ejecución del endpoint ----------
        mockMvc.perform(get("/payments/client/{clientId}", 1L)) // invoca GET /payments/client/1
                .andExpect(status().isOk())                                           // espera respuesta 200 OK
                .andExpect(jsonPath("$.length()").value(1))    // la lista devuelta debe tener 1 elemento
                // Validaciones sobre el primer pago en la lista
                .andExpect(jsonPath("$[0].id").value(payment.getId()))
                .andExpect(jsonPath("$[0].clientId").value(1L)); // se expone clientId en lugar del objeto completo

        // ---------- Verificación de interacción ----------
        // Confirma que el servicio fue invocado con el parámetro clientId=1.
        verify(paymentService).getPaymentsByClientId(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void registerPayment() throws Exception {
        // Simula que el test se ejecuta con un usuario autenticado con rol USER
        // (según la configuración de seguridad, solo el admin puede registrar pagos).

        // ---------- Datos de entrada ----------
        RegisterPaymentRequest request = new RegisterPaymentRequest();
        request.setAmount(100.0);
        request.setPaymentDate(LocalDate.of(2024, 1, 1));
        request.setDuration(RegisterPaymentRequest.Duration.QUINCE_DIAS);

        // Se construye el objeto request que se enviará al endpoint:
        // - Monto del pago: 100
        // - Fecha de pago: 01/01/2024
        // - Duración: 15 días (para calcular la fecha de expiración)

        // ---------- Objeto de respuesta simulado ----------
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setAmount(100.0);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setExpirationDate(request.getPaymentDate().plusDays(15));
        payment.setPaymentState(PaymentState.UP_TO_DATE);

        // Se crea un cliente asociado con id = 1
        Client client = new Client();
        ReflectionTestUtils.setField(client, "id", 1L);
        payment.setClient(client);

        // Se define el comportamiento del mock del servicio:
        // cuando se invoque a registerPayment con clientId=1 y cualquier fecha/monto,
        // debe devolver el objeto "payment" armado arriba.
        when(paymentService.registerPayment(eq(1L), any(LocalDate.class), any(LocalDate.class), anyDouble()))
                .thenReturn(payment);

        // ---------- Ejecución del endpoint ----------
        mockMvc.perform(post("/payments/client/{clientId}", 1L) // invoca POST /payments/client/1
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // envía el JSON del request
                .andExpect(status().isOk()) // Se espera respuesta 200 OK
                // ---------- Validaciones del JSON devuelto ----------
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.paymentDate").value(payment.getPaymentDate().toString()))
                .andExpect(jsonPath("$.expirationDate").value(payment.getExpirationDate().toString()))
                .andExpect(jsonPath("$.amount").value(payment.getAmount()))
                .andExpect(jsonPath("$.paymentState").value(payment.getPaymentState().toString()))
                .andExpect(jsonPath("$.clientId").value(1L))
                .andExpect(jsonPath("$.client").doesNotExist());
        // Se valida que el objeto completo "client" no venga en la respuesta (evita exponer datos de cliente).

        // ---------- Verificación de interacción ----------
        // Se comprueba que el método registerPayment del servicio fue invocado
        // con clientId=1 y los demás parámetros esperados.
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