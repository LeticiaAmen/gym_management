package com.gym.gym_management.security;

import com.gym.gym_management.controller.PaymentController;
import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // base64("thisisaverysolongjwt-secret-text-mock")
        "jwt.secret=dGhpc2lzYXZlcnlzb2xvbmdqd3Qtc2VjcmV0LXRleHQtbW9jaw=="
})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void endpointPagos_requiereRolAdmin_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void endpointPagos_requiereRolAdmin_conRolInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void endpointPagos_conRolAdminDevuelve200() throws Exception {
        Mockito.when(paymentService.findPayments(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointsPublicos_permitenAccesoSinAuth_index() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointsPublicos_permitenAccesoSinAuth_login() throws Exception {
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isUnauthorized());
    }
}

