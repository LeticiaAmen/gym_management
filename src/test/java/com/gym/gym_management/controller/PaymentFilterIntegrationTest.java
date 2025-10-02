package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.model.*;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración para validar el filtrado de /api/payments?state=...
 * Comprueba:
 *  - Filtra correctamente UP_TO_DATE, EXPIRED y VOIDED.
 *  - Acepta variantes flexibles ("al dia", "vencido", "anulado").
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payfilter;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "JWT_SECRET=UGF5bWVudEZpbHRlclNlY3JldEJhc2U2NA==",
        "app.email.enabled=false"
})
@WithMockUser(roles = "ADMIN")
class PaymentFilterIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private IPaymentRepository paymentRepository;
    @Autowired private IClientRepository clientRepository;
    @Autowired private ObjectMapper objectMapper;

    private Long clientUpToDateId;
    private Long clientExpiredId;
    private Long clientVoidedId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        clientRepository.deleteAll();

        Client c1 = new Client("Ana","Activa","ana@test.com","111");
        c1.setActive(true); clientRepository.save(c1); clientUpToDateId = c1.getId();

        Client c2 = new Client("Ernesto","Expirado","exp@test.com","222");
        c2.setActive(true); clientRepository.save(c2); clientExpiredId = c2.getId();

        Client c3 = new Client("Valeria","Voided","void@test.com","333");
        c3.setActive(true); clientRepository.save(c3); clientVoidedId = c3.getId();

        // UP_TO_DATE: fecha de pago ayer, expira en +10 días
        Payment p1 = new Payment();
        p1.setClient(c1);
        p1.setAmount(5000.0);
        p1.setMethod(PaymentMethod.CASH);
        p1.setMonth(LocalDate.now().getMonthValue());
        p1.setYear(LocalDate.now().getYear());
        p1.setPaymentDate(LocalDate.now().minusDays(1));
        p1.setExpirationDate(LocalDate.now().plusDays(10));
        p1.setState(PaymentState.UP_TO_DATE);
        paymentRepository.save(p1);

        // EXPIRED: expiró hace 5 días
        Payment p2 = new Payment();
        p2.setClient(c2);
        p2.setAmount(6000.0);
        p2.setMethod(PaymentMethod.CREDIT);
        p2.setMonth(LocalDate.now().getMonthValue());
        p2.setYear(LocalDate.now().getYear());
        p2.setPaymentDate(LocalDate.now().minusDays(20));
        p2.setExpirationDate(LocalDate.now().minusDays(5));
        p2.setState(PaymentState.EXPIRED);
        paymentRepository.save(p2);

        // VOIDED: pago anulado
        Payment p3 = new Payment();
        p3.setClient(c3);
        p3.setAmount(7000.0);
        p3.setMethod(PaymentMethod.DEBIT);
        p3.setMonth(LocalDate.now().getMonthValue());
        p3.setYear(LocalDate.now().getYear());
        p3.setPaymentDate(LocalDate.now().minusDays(3));
        p3.setExpirationDate(LocalDate.now().plusDays(25));
        p3.setState(PaymentState.VOIDED);
        p3.setVoided(true);
        paymentRepository.save(p3);
    }

    @Test
    @DisplayName("Filtra UP_TO_DATE")
    void filtraUpToDate() throws Exception {
        JsonNode content = queryState("UP_TO_DATE");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("state").asText()).isEqualTo("UP_TO_DATE");
    }

    @Test
    @DisplayName("Filtra EXPIRED con variante 'vencido'")
    void filtraExpiredVariante() throws Exception {
        JsonNode content = queryState("vencido");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("state").asText()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Filtra VOIDED con variante 'anulado'")
    void filtraVoidedVariante() throws Exception {
        JsonNode content = queryState("anulado");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("state").asText()).isEqualTo("VOIDED");
    }

    @Test
    @DisplayName("Variante 'al dia' → UP_TO_DATE")
    void varianteAlDia() throws Exception {
        JsonNode content = queryState("al dia");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("state").asText()).isEqualTo("UP_TO_DATE");
    }

    @Test
    @DisplayName("Estado desconocido devuelve los 3 pagos (sin filtro)")
    void estadoDesconocido() throws Exception {
        JsonNode content = queryState("desconocidoXYZ");
        // sin filtro -> page con los 3
        assertThat(content).hasSize(3);
    }

    private JsonNode queryState(String state) throws Exception {
        String url = "/api/payments?state=" + state + "&size=20";
        MvcResult res = mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        // El endpoint devuelve un Page -> tomar root.content
        JsonNode content = root.get("content");
        assertThat(content).isNotNull();
        return content;
    }
}

