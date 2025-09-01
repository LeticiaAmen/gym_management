package com.gym.gym_management.service;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.repository.IClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que encapsula la lógica de negocio para la gestión de clientes.
 *
 * Funciones principales:
 * - Consultar todos los clientes.
 * - Guardar un nuevo cliente.
 * - Actualizar un cliente existente.
 * - Eliminar un cliente por su ID.
 *
 * Relación con los requerimientos:
 * - "Gestión de Clientes" (Administradores):
 *   Permite crear, modificar, eliminar y consultar clientes.
 * - Actúa como intermediario entre ClientController y IClientRepository,
 *   manteniendo una separación clara entre la capa de presentación y la capa de persistencia.
 */
@Service
public class ClientService {
    // Repositorio para acceder a los datos de clientes en la base de datos.
    @Autowired
    private IClientRepository clientRepository;

    //Codificador de contraseñas para almacenar passwords
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Obtiene todos los clientes registrados en la base de datos.
     * @return lista completa de clientes.
     */
    public List<Client> findAll(){
        return clientRepository.findAll();
    }

    /**
     * Guarda un nuevo cliente o actualiza uno existente.
     *
     * @param client objeto Client a persistir.
     * @return cliente guardado con su ID generado (si es nuevo).
     */
    public Client saveClient(Client client){
        return clientRepository.save(client);
    }

    /**
     * Marca un cliente como inactivo de manera idempotente.
     * Si el cliente ya está inactivo, no se realizan cambios adicionales.
     *
     * @param id identificador del cliente a desactivar.
     * @return el cliente actualizado tras la operación.
     */
    public Client deactivateClient(Long id){
        Client client = clientRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("No se encuentra el cliente a desactivar"));

        if (client.isActive()){
            client.setActive(false);
            client = clientRepository.save(client);
        }
        return client;
    }

    /**
     * Marca un cliente como activo de manera idempotente.
     * Si el cliente ya está activo, no se realizan cambios adicionales.
     *
     * @param id identificador del cliente a activar.
     * @return el cliente actualizado tras la operación.
     */
    public Client activateClient(Long id){
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encuentra el cliente a activar"));

        if (!client.isActive()){
            client.setActive(true);
            client = clientRepository.save(client);
        }
        return client;
    }

    /**
     * Elimina (soft-delete) baja lógica de un cliente según su identificador
     * se delega a deactivateclient para preservar el historial del cliente.
     */
    public void deleteClient(Long id){
        deactivateClient(id);
    }


    /**
     * Actualiza un cliente existente.
     * En este diseño, es equivalente a saveClient(), pero se mantiene para claridad semántica.
     * @param client objeto Client con los datos actualizados.
     */
    public void update(Client client){
        if (client.getUser() != null && client.getUser().getPassword()!= null){
           client.getUser().setPassword(passwordEncoder.encode(client.getUser().getPassword()));
        }
        clientRepository.save(client);
    }

}
