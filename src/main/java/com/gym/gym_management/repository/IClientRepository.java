package com.gym.gym_management.repository;

import com.gym.gym_management.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IClientRepository extends JpaRepository<Client, Long> {

    @Query("SELECT c FROM Client c WHERE c.isActive = true")
    List<Client> findAllActive();

    @Query("SELECT c FROM Client c WHERE c.email = ?1")
    Client findByEmail(String email);

    boolean existsByEmail(String email);
}
