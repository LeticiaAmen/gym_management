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
import java.time.YearMonth;
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
        if (dto.getMethod() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
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
        // Fecha de pago (opcional). Prohibir futura si negocio lo requiere
        LocalDate payDate = dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now();
        if (dto.getPaymentDate() != null && payDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser futura");
        }

        // Calcular expirationDate: mensual (1 mes) o por días (durationDays)
        Integer durationDays = dto.getDurationDays();
        LocalDate expiration;
        if (durationDays != null) {
            if (durationDays < 1) throw new IllegalArgumentException("La duración debe ser al menos 1 día");
            expiration = payDate.plusDays(durationDays);
        } else {
            // Mensual: hasta el mismo día del mes siguiente (ej. 15/08 → 15/09)
            expiration = payDate.plusMonths(1);
        }

        Payment payment = fromDTO(dto);
        payment.setClient(client);
        payment.setState(PaymentState.UP_TO_DATE);
        payment.setPaymentDate(payDate);
        payment.setExpirationDate(expiration);

        Payment saved = paymentRepository.save(payment);
        auditService.logPaymentCreation(saved); // Auditoría mínima
        PaymentDTO out = toDTO(saved);
        out.setExpirationDate(expiration);
        return out;
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

    // Recordatorios
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

    // ===== Estado de período (sin persistir) =====
    public LocalDate computeDueDate(int month, int year) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        if (year < 2000) throw new IllegalArgumentException("El año debe ser >= 2000");
        YearMonth ym = YearMonth.of(year, month);
        int day = Math.min(10, ym.lengthOfMonth());
        return LocalDate.of(year, month, day);
    }

    public PaymentState computePeriodState(Long clientId, int month, int year) {
        if (clientId == null) throw new IllegalArgumentException("clientId es obligatorio");
        if (!clientRepository.existsById(clientId)) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        if (paymentRepository.existsByClient_IdAndMonthAndYearAndVoidedFalse(clientId, month, year)) {
            return PaymentState.UP_TO_DATE;
        }
        LocalDate due = computeDueDate(month, year);
        LocalDate graceEnd = due.plusDays(3);
        LocalDate today = LocalDate.now();
        return today.isAfter(graceEnd) ? PaymentState.EXPIRED : PaymentState.PENDING;
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
        dto.setExpirationDate(payment.getExpirationDate());
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
