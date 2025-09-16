package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.controller.dto.ClientDTO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void creaCliente_validaEntradasYDevuelve201ConLocation() throws Exception {
        ClientDTO input = new ClientDTO();
        input.setFirstName("Ana");
        input.setLastName("García");
        input.setEmail("ana@example.com");
        input.setPhone("123");

        ClientDTO created = new ClientDTO();
        created.setId(10L);
        created.setFirstName("Ana");
        created.setLastName("García");
        created.setEmail("ana@example.com");
        created.setPhone("123");

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
}
