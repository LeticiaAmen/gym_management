package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.repository.IClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AuditService auditService;

    // Listado simple sin filtros
    public List<ClientDTO> findAll() {
        return clientRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // Búsqueda con filtros opcionales
    public List<ClientDTO> search(String q, Boolean active, PaymentState paymentState) {
        String text = (q == null || q.isBlank()) ? null : q.trim();
        List<Client> base;
        if (text == null && active != null) {
            base = clientRepository.findByActive(active);
        } else if (text != null || active != null) { // si hay texto o active (aunque sea null+texto)
            base = clientRepository.search(text, active);
        } else {
            base = clientRepository.findAll();
        }
        if (paymentState != null) {
            LocalDate today = LocalDate.now();
            int month = today.getMonthValue();
            int year = today.getYear();
            base = base.stream()
                    .filter(c -> paymentService.computePeriodState(c.getId(), month, year) == paymentState)
                    .collect(Collectors.toList());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ClientDTO findById(Long id) {
        return clientRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    public ClientDTO create(ClientDTO dto) {
        // Validación de email único
        if (dto.getEmail() != null && clientRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El mail ya está registrado");
        }
        Client client = fromDTO(dto);
        client.setActive(true);
        Client saved = clientRepository.save(client);
        auditService.logClientCreation(saved);
        return toDTO(saved);
    }

    public ClientDTO update(Long id, ClientDTO dto) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        // Validación de email único (excluye el propio id)
        if (dto.getEmail() != null && clientRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El mail ya está registrado");
        }

        Client original = copyOf(client); // Para auditoría
        updateClientFromDTO(client, dto);
        Client updated = clientRepository.save(client);
        auditService.logClientUpdate(original, updated);
        return toDTO(updated);
    }

    // Operaciones de negocio
    public void deactivate(Long id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        client.setActive(false);
        clientRepository.save(client);
        auditService.logClientDeactivation(client);
    }

    public void activate(Long id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        client.setActive(true);
        clientRepository.save(client);
        // TODO: agregar log de auditoría específico si se requiere
    }

    public ClientDTO pause(Long id, LocalDate from, LocalDate to, String reason) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        client.setPausedFrom(from);
        client.setPausedTo(to);
        client.setPauseReason(reason);
        Client updated = clientRepository.save(client);
        auditService.logClientPause(updated);
        return toDTO(updated);
    }

    public ClientDTO resume(Long id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        client.setPausedFrom(null);
        client.setPausedTo(null);
        client.setPauseReason(null);
        Client updated = clientRepository.save(client);
        auditService.logClientResume(updated);
        return toDTO(updated);
    }

    // Métodos para dashboard
    public long countActiveClients() {
        return clientRepository.countByIsActiveTrue();
    }

    // Métodos auxiliares
    private ClientDTO toDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setFirstName(client.getFirstName());
        dto.setLastName(client.getLastName());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        dto.setActive(client.isActive());
        dto.setStartDate(client.getStartDate());
        dto.setNotes(client.getNotes());
        dto.setPausedFrom(client.getPausedFrom());
        dto.setPausedTo(client.getPausedTo());
        dto.setPauseReason(client.getPauseReason());
        return dto;
    }

    private Client fromDTO(ClientDTO dto) {
        Client client = new Client();
        updateClientFromDTO(client, dto);
        return client;
    }

    private void updateClientFromDTO(Client client, ClientDTO dto) {
        client.setFirstName(dto.getFirstName());
        client.setLastName(dto.getLastName());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        if (dto.getStartDate() != null) {
            client.setStartDate(dto.getStartDate());
        }
        client.setNotes(dto.getNotes());
    }

    private Client copyOf(Client c) {
        Client copy = new Client();
        copy.setId(c.getId());
        copy.setFirstName(c.getFirstName());
        copy.setLastName(c.getLastName());
        copy.setEmail(c.getEmail());
        copy.setPhone(c.getPhone());
        copy.setActive(c.isActive());
        copy.setNotes(c.getNotes());
        copy.setStartDate(c.getStartDate());
        copy.setPausedFrom(c.getPausedFrom());
        copy.setPausedTo(c.getPausedTo());
        copy.setPauseReason(c.getPauseReason());
        return copy;
    }
}
