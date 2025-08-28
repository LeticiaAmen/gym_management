package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Clase de pruebas de {@link ClientController} que verifica sus endpoints
 * utilizando {@link MockMvc} para simular llamadas HTTP y Mockito para aislar
 * la capa de servicio.
 */


@SpringBootTest
@AutoConfigureMockMvc
class ClientControllerTest {

    /** Simula peticiones HTTP hacia el controlador. */
    @Autowired
    private MockMvc mockMvc;

    /** Convierte objetos Java a JSON y viceversa dentro de las pruebas. */
    @Autowired
    private ObjectMapper objectMapper;

    /** Servicio de clientes simulado para aislar la lógica del controlador. */
    @MockBean
    private ClientService clientService;

    @Test
    @WithMockUser
    void findAll() throws Exception {
        //verifica que el endpoint retorne la lista completa de clientes

        // Preparación de datos: se crea una lista con dos clientes de ejemplo.
        List<Client> clients = List.of(
                new Client(User.builder().id(1L).email("john@example.com").password("pass").role(Role.CLIENT).build(),
                        "John", "Doe", "123", true, new ArrayList<>()),
                new Client(User.builder().id(2L).email("jane@example.com").password("pass").role(Role.CLIENT).build(),
                        "Jane", "Smith", "456", true, new ArrayList<>())
        );

        // Configuración del mock: se retorna la lista anterior cuando se llame al servicio.
        when(clientService.findAll()).thenReturn(clients);

        // Llamada a MockMvc: se ejecuta una solicitud GET al endpoint /clients.
        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"));

        // Verificación: se asegura que el servicio fue invocado una sola vez.
        verify(clientService).findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void save() throws Exception {
        // Comprueba que el endpoint permita guardar un nuevo cliente

        // Preparación de datos: se crea un cliente que será enviado en la solicitud.
        Client client = new Client(User.builder().id(1L).email("john@example.com").password("pass").role(Role.CLIENT).build(),
                "John", "Doe", "123", true, new ArrayList<>());

        // Configuración del mock: el servicio devuelve el mismo cliente al guardar.
        when(clientService.saveClient(any(Client.class))).thenReturn(client);

        //  Llamada a MockMvc: se ejecuta un POST con el JSON del cliente.
        mockMvc.perform(post("/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        // Verificación: se comprueba que el servicio guardó el cliente.
        verify(clientService).saveClient(any(Client.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void update() throws Exception{
        // Valida que el endpoint actualice un cliente existente.

        // Preparación de datos: se construye un cliente con información nueva.
        Client client = new Client(User.builder().id(1L).email("john@example.com").password("pass").role(Role.CLIENT).build(),
                "John", "Doe", "123", true, new ArrayList<>());

        // Configuración del mock: no se realiza ninguna acción adicional al actualizar.
        doNothing().when(clientService).update(any(Client.class));

        // Llamada a MockMvc: se envía la información mediante una solicitud PUT.
        mockMvc.perform(put("/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(client)))
                .andExpect(status().isOk());

        // Verificación: se asegura que el servicio recibió el cliente para actualizarlo.
        verify(clientService).update(any(Client.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteClient() throws Exception {
        // Verifica que se elimine un cliente existente por su ID.

        // Preparación de datos: se define el identificador del cliente a eliminar.
        Long id = 1L;

        // Configuración del mock: el servicio no realiza acción al eliminar.
        doNothing().when(clientService).deleteClient(id);

        // Llamada a MockMvc: se ejecuta una solicitud DELETE con el ID como path variable.
        mockMvc.perform(delete("/clients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().string("Se eliminó el cliente con id: " + id));

        // Verificación: se comprueba que el servicio eliminó al cliente correspondiente.
        verify(clientService).deleteClient(id);
    }
}