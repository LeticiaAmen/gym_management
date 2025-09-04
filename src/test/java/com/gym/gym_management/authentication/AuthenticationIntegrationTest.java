package com.gym.gym_management.authentication;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthenticationIntegrationTest {

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savingClientCascadesUser() {
        // Se construye un objeto User usando el patrón Builder.
        // Este usuario tendrá el rol CLIENT y será el que se asocie al cliente.
        User user = User.builder()
                .email("user@example.com")
                .password("pass")
                .role(Role.CLIENT)
                .build();

        // Se crea un cliente asociado al usuario anterior.
        // El constructor de Client recibe al User y demás datos de la persona.
        // El "true" indica que el cliente está activo.
        Client client = new Client(user, "Jane", "Doe", "123", true, new ArrayList<>());

        // Al guardar el cliente en el repositorio, gracias al mapeo y las
        // configuraciones de cascada (cascade = CascadeType.ALL en la entidad Client),
        // también se persiste automáticamente el User asociado.
        clientRepository.save(client);

        // Se fuerza la escritura inmediata a la base de datos (flush)
        // y luego se limpia el contexto de persistencia (clear)
        // para asegurarnos de que la siguiente consulta sea realmente a la BD.
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
    }
}