package com.gym.gym_management.web.client;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mi-cuenta")
public class ClientPageController {
    private final IClientRepository clientRepo;
    private final PaymentService paymentService;

    public ClientPageController(IClientRepository clientRepo, PaymentService paymentService) {
        this.clientRepo = clientRepo;
        this.paymentService = paymentService;
    }

    @GetMapping("/pagos")
    public String myPayments(Authentication auth, Model model) {
        return "client/payments";
    }
}
