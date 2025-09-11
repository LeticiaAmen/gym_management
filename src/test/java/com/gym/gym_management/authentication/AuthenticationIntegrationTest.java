package com.gym.gym_management.authentication;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.repository.IClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthenticationIntegrationTest {

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savingClientCascadesUser() {
        // Ahora el modelo es admin-only: Client ya no está vinculado a User.
        // Simplemente guardamos un Client y verificamos que se persiste y puede recuperarse por email.
        Client client = new Client("Jane", "Doe", "user@example.com", "123456");
        clientRepository.save(client);

        entityManager.flush();
        entityManager.clear();

        Client found = clientRepository.findByEmail("user@example.com");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("user@example.com");
    }
}