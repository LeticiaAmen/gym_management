package com.gym.gym_management.controller;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public List<Payment> findAll(){
        return paymentService.findAll();
    }

    @GetMapping("{id}")
    public ResponseEntity<Payment> findById(@PathVariable Long id){
        Optional<Payment> payment = paymentService.findById(id);
        if(payment.isPresent()){
            return ResponseEntity.ok(payment.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Payment save(@RequestBody Payment payment){
        return paymentService.registerPayment(payment);
    }

   @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) throws Exception{
       paymentService.delete(id);
       return ResponseEntity.ok("Se eliminó el pago con id: " + id);


   }

}
