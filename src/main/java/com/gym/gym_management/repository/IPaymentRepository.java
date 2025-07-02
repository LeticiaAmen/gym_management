package com.gym.gym_management.repository;

import com.gym.gym_management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByClientId(Long clientId);
}
