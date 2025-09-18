package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    @Mock private ClientService clientService;
    @InjectMocks private ClientController controller;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private ClientDTO sample(long id, String fn, boolean active) {
        ClientDTO c = new ClientDTO();
        c.setId(id);
        c.setFirstName(fn);
        c.setLastName("Test");
        c.setEmail("u"+id+"@ex.com");
        c.setPhone("123");
        c.setActive(active);
        return c;
    }

    @Test
    void creaCliente_validaEntradasYDevuelve201ConLocation() throws Exception {
        ClientDTO input = new ClientDTO();
        input.setFirstName("Ana");
        input.setLastName("García");
        input.setEmail("ana@example.com");
        input.setPhone("123");

        ClientDTO created = sample(10L, "Ana", true);

        when(clientService.create(any(ClientDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void creaCliente_emailInvalidoDevuelve400() throws Exception {
        ClientDTO input = new ClientDTO();
        input.setFirstName("Pe");
        input.setLastName("Lo");
        input.setEmail("no-es-email");
        input.setPhone("123");

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desactivaCliente_devuelve204() throws Exception {
        doNothing().when(clientService).deactivate(5L);

        mockMvc.perform(delete("/api/clients/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void desactivaCliente_inexistenteDevuelve404() throws Exception {
        doThrow(new IllegalArgumentException("Cliente no encontrado")).when(clientService).deactivate(999L);

        mockMvc.perform(delete("/api/clients/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Cliente no encontrado"));
    }

    // ================= NUEVOS TESTS DE FILTROS =================

    @Test
    void listaClientes_filtraPorActiveTrue() throws Exception {
        List<ClientDTO> result = List.of(sample(1,"A", true), sample(2,"B", true));
        when(clientService.search(null, true, null)).thenReturn(result);

        mockMvc.perform(get("/api/clients?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true));

        verify(clientService).search(null, true, null);
        verify(clientService, never()).findAll();
    }

    @Test
    void listaClientes_activeVacioUsaFindAll() throws Exception {
        List<ClientDTO> result = List.of(sample(3,"C", true));
        when(clientService.findAll()).thenReturn(result);

        mockMvc.perform(get("/api/clients?active="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));

        verify(clientService).findAll();
        verify(clientService, never()).search(any(), any(), any());
    }

    @Test
    void listaClientes_paymentVacioUsaFindAll() throws Exception {
        List<ClientDTO> result = List.of(sample(4,"D", true));
        when(clientService.findAll()).thenReturn(result);

        mockMvc.perform(get("/api/clients?payment="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));

        verify(clientService).findAll();
    }

    @Test
    void listaClientes_filtraPorPaymentExpired() throws Exception {
        List<ClientDTO> result = List.of(sample(5,"E", true));
        when(clientService.search(null, null, PaymentState.EXPIRED)).thenReturn(result);

        mockMvc.perform(get("/api/clients?payment=EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));

        verify(clientService).search(null, null, PaymentState.EXPIRED);
    }

    @Test
    void listaClientes_activeInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/clients?active=abc"))
                .andExpect(status().isBadRequest());
        verify(clientService, never()).search(any(), any(), any());
        verify(clientService, never()).findAll();
    }

    @Test
    void listaClientes_paymentInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/clients?payment=FOO"))
                .andExpect(status().isBadRequest());
        verify(clientService, never()).search(any(), any(), any());
        verify(clientService, never()).findAll();
    }
}
