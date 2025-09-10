package com.gym.gym_management.web.admin;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.service.ClientService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/clients")
public class ClientFormController {
    private final ClientService clientService;

    public ClientFormController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/new")
    public String newClient(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("pageTitle", "Nuevo Cliente");
        return "admin/client-form";
    }

}
