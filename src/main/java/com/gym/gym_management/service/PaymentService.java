package com.gym.gym_management.service;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {
    @Autowired
    private IPaymentRepository paymentRepository;

    public List<Payment> getPaymentsByClientId(Long clientId){
        return paymentRepository.findByClientId(clientId);
    }

    public Payment registerPayment(Payment payment){
        return paymentRepository.save(payment);
    }

    public List<Payment> findAll(){
        return paymentRepository.findAll();
    }

    public Optional<Payment> findById(Long id){
        return paymentRepository.findById(id);
    }

    public void delete(Long id) throws Exception {
        Optional<Payment> payment = paymentRepository.findById(id);
        if(payment.isPresent()){
            paymentRepository.deleteById(id);
        }else {
            throw new Exception("No se pudo eliminar el pago con id: " + id);
        }
    }

}
