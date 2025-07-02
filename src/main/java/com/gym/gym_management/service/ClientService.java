package com.gym.gym_management.service;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.repository.IClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {
    @Autowired
    private IClientRepository clientRepository;

    public List<Client> findAll(){
        return clientRepository.findAll();
    }

    public Client saveClient(Client client){
        return clientRepository.save(client);
    }

    public void deleteClient(Long id){
        clientRepository.deleteById(id);
    }

    public void update(Client client){
        clientRepository.save(client);
    }

}
