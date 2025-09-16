package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.ClientDTO;
import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private IClientRepository clientRepository;
    @Mock private IPaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;
    @Mock private AuditService auditService;

    @InjectMocks private ClientService clientService;

    private Client c1;
    private Client c2;

    @BeforeEach
    void setUp() {
        c1 = new Client();
        c1.setId(1L);
        c1.setFirstName("Ana");
        c1.setLastName("García");
        c1.setEmail("ana@example.com");
        c1.setActive(true);
        c1.setStartDate(LocalDate.now().minusMonths(2));

        c2 = new Client();
        c2.setId(2L);
        c2.setFirstName("Juan");
        c2.setLastName("Pérez");
        c2.setEmail("juan@example.com");
        c2.setActive(false);
        c2.setStartDate(LocalDate.now().minusMonths(1));
    }

    @Test
    void findAll_mapsEntitiesToDTO() {
        given(clientRepository.findAll()).willReturn(Arrays.asList(c1, c2));
        List<ClientDTO> out = clientService.findAll();
        assertThat(out).hasSize(2);
        assertThat(out.get(0).getEmail()).isEqualTo("ana@example.com");
        assertThat(out.get(1).isActive()).isFalse();
    }

    @Test
    void search_filtersByDerivedPaymentState() {
        // base list returned by repository (sin filtros -> usa findAll())
        given(clientRepository.findAll()).willReturn(Arrays.asList(c1, c2));
        // payment state derived by month/year for each client
        LocalDate today = LocalDate.now();
        given(paymentService.computePeriodState(eq(1L), eq(today.getMonthValue()), eq(today.getYear())))
                .willReturn(PaymentState.UP_TO_DATE);
        given(paymentService.computePeriodState(eq(2L), eq(today.getMonthValue()), eq(today.getYear())))
                .willReturn(PaymentState.PENDING);

        List<ClientDTO> result = clientService.search(null, null, PaymentState.UP_TO_DATE);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findById_returnsDTO_orThrows() {
        given(clientRepository.findById(1L)).willReturn(Optional.of(c1));
        ClientDTO dto = clientService.findById(1L);
        assertThat(dto.getEmail()).isEqualTo("ana@example.com");

        given(clientRepository.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> clientService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente no encontrado");
    }

    @Test
    void create_validatesUniqueEmail_setsActive_logsAudit() {
        ClientDTO in = new ClientDTO();
        in.setFirstName("Ana");
        in.setLastName("García");
        in.setEmail("ana@example.com");
        given(clientRepository.existsByEmail("ana@example.com")).willReturn(false);
        given(clientRepository.save(any(Client.class))).willAnswer(i -> {
            Client saved = i.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ClientDTO out = clientService.create(in);
        assertThat(out.getId()).isEqualTo(10L);
        verify(auditService).logClientCreation(any(Client.class));

        // conflict path
        given(clientRepository.existsByEmail("dup@example.com")).willReturn(true);
        ClientDTO dup = new ClientDTO();
        dup.setEmail("dup@example.com");
        assertThatThrownBy(() -> clientService.create(dup))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void update_updatesFields_andAudits() {
        given(clientRepository.findById(1L)).willReturn(Optional.of(c1));
        given(clientRepository.existsByEmailAndIdNot("nuevo@example.com", 1L)).willReturn(false);
        given(clientRepository.save(any(Client.class))).willAnswer(i -> i.getArgument(0));

        ClientDTO dto = new ClientDTO();
        dto.setFirstName("NuevoNombre");
        dto.setLastName("NuevoApellido");
        dto.setEmail("nuevo@example.com");
        dto.setPhone("123");
        ClientDTO out = clientService.update(1L, dto);

        assertThat(out.getFirstName()).isEqualTo("NuevoNombre");
        verify(auditService).logClientUpdate(any(Client.class), any(Client.class));

        // duplicate email path
        given(clientRepository.existsByEmailAndIdNot("dup@example.com", 1L)).willReturn(true);
        dto.setEmail("dup@example.com");
        assertThatThrownBy(() -> clientService.update(1L, dto))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void deactivate_setsInactive_andAudits() {
        given(clientRepository.findById(2L)).willReturn(Optional.of(c2));
        clientService.deactivate(2L);
        assertThat(c2.isActive()).isFalse();
        verify(clientRepository).save(c2);
        verify(auditService).logClientDeactivation(c2);
    }

    @Test
    void countActiveClients_delegates() {
        given(clientRepository.countByIsActiveTrue()).willReturn(5L);
        assertThat(clientService.countActiveClients()).isEqualTo(5L);
    }
}
