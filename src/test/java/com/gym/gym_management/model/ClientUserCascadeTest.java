package com.gym.gym_management.model;

import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ClientUserCascadeTest {

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savingClientPropagatesChangesToUser(){
        User user = new User(null, "old@example.com", "pass", Role.CLIENT);
        Client client = new Client(user,"John", "Doe", "123", true, new ArrayList<>());
        clientRepository.save(client);
        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(client.getId()).orElseThrow();
        persisted.getUser().setEmail("new@example.com");
        clientRepository.save(persisted);
        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }
}
