package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.model.*;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private AuditService auditService;

    // Registrar pago con validaciones de dominio e idempotencia
    public PaymentDTO registerPayment(PaymentDTO dto) {
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        if (dto.getMonth() == null || dto.getMonth() < 1 || dto.getMonth() > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        if (dto.getYear() == null || dto.getYear() < 2000) {
            throw new IllegalArgumentException("El año debe ser >= 2000");
        }
        var client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (!client.isActive()) {
            throw new IllegalStateException("El cliente está inactivo");
        }
        // Idempotencia por período (ignorando pagos anulados)
        if (paymentRepository.existsByClient_IdAndMonthAndYearAndVoidedFalse(dto.getClientId(), dto.getMonth(), dto.getYear())) {
            throw new IllegalStateException("Ya existe un pago válido para ese período");
        }
        // Fecha de pago (opcional). Puedes prohibir futura si el negocio lo requiere
        if (dto.getPaymentDate() != null && dto.getPaymentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser futura");
        }

        Payment payment = fromDTO(dto);
        payment.setClient(client);
        payment.setState(PaymentState.UP_TO_DATE);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }

        Payment saved = paymentRepository.save(payment);
        auditService.logPaymentCreation(saved); // Auditoría mínima
        return toDTO(saved);
    }

    // Consulta con filtros + paginación
    public Page<PaymentDTO> findPayments(Long clientId, LocalDate from, LocalDate to, PaymentState state, Pageable pageable) {
        return paymentRepository.findByFilters(clientId, from, to, state, pageable)
                .map(this::toDTO);
    }

    public Optional<PaymentDTO> findDTOById(Long id) {
        return paymentRepository.findById(id).map(this::toDTO);
    }

    // Anular pago con auditoría y admin actual
    public PaymentDTO voidPayment(Long id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        if (payment.isVoided() || payment.getState() == PaymentState.VOIDED) {
            throw new IllegalStateException("El pago ya fue anulado");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = null;
        if (auth != null) {
            String email = auth.getName();
            adminId = userRepository.findByEmail(email).map(User::getId).orElse(null);
        }
        payment.setVoided(true);
        payment.setState(PaymentState.VOIDED);
        payment.setVoidedAt(LocalDateTime.now());
        payment.setVoidedBy(adminId);
        payment.setVoidReason(reason);

        Payment saved = paymentRepository.save(payment);
        auditService.logPaymentVoid(saved, reason);
        return toDTO(saved);
    }

    // Recordatorios (utilizados por EmailService cuando está habilitado)
    public List<Payment> findExpiringPayments() {
        return paymentRepository.findByExpirationDateAndPaymentState(
                LocalDate.now().plusDays(3),
                PaymentState.UP_TO_DATE
        );
    }

    public List<Payment> findOverduePayments() {
        return paymentRepository.findByExpirationDateBeforeAndPaymentState(
                LocalDate.now(),
                PaymentState.UP_TO_DATE
        );
    }

    // Mapeos
    private PaymentDTO toDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setClientId(payment.getClient().getId());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setMonth(payment.getMonth());
        dto.setYear(payment.getYear());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setState(payment.getState());
        dto.setVoided(payment.isVoided());
        dto.setVoidedBy(payment.getVoidedBy());
        dto.setVoidReason(payment.getVoidReason());
        return dto;
    }

    private Payment fromDTO(PaymentDTO dto) {
        Payment payment = new Payment();
        payment.setAmount(dto.getAmount());
        payment.setMethod(dto.getMethod());
        payment.setMonth(dto.getMonth());
        payment.setYear(dto.getYear());
        payment.setPaymentDate(dto.getPaymentDate());
        return payment;
    }
}
