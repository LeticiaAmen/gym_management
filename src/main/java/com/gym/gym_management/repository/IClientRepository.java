package com.gym.gym_management.repository;

import com.gym.gym_management.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IClientRepository extends JpaRepository<Client, Long> {
    Client findByUserEmail(String email);
}
