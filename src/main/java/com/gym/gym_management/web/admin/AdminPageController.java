package com.gym.gym_management.web.admin;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminPageController {
    private final ClientService clientService;

    public AdminPageController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/clients")
    public String clientsPage(Model model) {
        List<Client> clients = clientService.findAll();
        model.addAttribute("clients", clients);
        model.addAttribute("pageTitle", "Clients");
        return "admin/clients";
    }
}
