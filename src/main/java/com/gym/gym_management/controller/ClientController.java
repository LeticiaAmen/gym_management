package com.gym.gym_management.controller;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;


    @GetMapping
    public List<Client> findAll(){
        return clientService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Client save(@RequestBody Client client){
        return clientService.saveClient(client);
    }
    @PutMapping
    public void update(@RequestBody Client client){
        clientService.update(client);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id){
        clientService.deleteClient(id);
        return ResponseEntity.ok("Se eliminó el cliente con id: " + id);
    }
}
