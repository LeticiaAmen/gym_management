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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio para la gestión integral de pagos de membresías.
 * <p>
 * Responsabilidades centrales:
 * <ul>
 *     <li><b>Registro de pagos</b>: con validaciones de negocio e idempotencia (cliente + mes + año únicos mientras no esté anulado).</li>
 *     <li><b>Cálculo de vencimiento</b>: mensual por defecto o personalizado por días (durationDays).</li>
 *     <li><b>Persistencia de estado</b>: se guardan estados {@code UP_TO_DATE}, {@code VOIDED}; el estado {@code EXPIRED} se materializa (persistido) mediante un job diario que ejecuta un bulk update.</li>
 *     <li><b>Anulación (void)</b>: marca lógica que preserva trazabilidad y evita borrar registros históricos.</li>
 *     <li><b>Consultas filtradas</b>: soporte de paginado y filtros por cliente, fechas y estado.</li>
 *     <li><b>Recordatorios</b>: obtención de pagos próximos a vencer (para disparar emails).</li>
 *     <li><b>Expiración masiva</b>: transición automática UP_TO_DATE -> EXPIRED cuando vencen (job externo llama {@link #expireOverduePayments()}).</li>
 * </ul>
 * Diseño y decisiones:
 * <ul>
 *     <li>No se persiste un estado "intermedio" (ej. PENDING). Mientras no expire ni se anule, se considera {@code UP_TO_DATE}.</li>
 *     <li>El cálculo del estado de un período sin pago registrado se simplifica a binario (antes / después de la gracia). Si se supera el período de gracia sin pago, se comunica como {@code EXPIRED} aunque no exista Payment para ese mes aún (útil para UI/reportes rápidos).</li>
 *     <li>El job de expiración garantiza consistencia histórica (estadísticas, integraciones futuras para acceso físico al gimnasio).</li>
 * </ul>
 */
@Service
public class PaymentService {

    /** Días de gracia luego del dueDate conceptual del período (negocio). */
    private static final int GRACE_DAYS = 3;

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Registra un nuevo pago validando reglas de negocio e impidiendo duplicados para el mismo período.
     * Flujo:
     * <ol>
     *   <li>Validar monto, método, mes y año.</li>
     *   <li>Verificar existencia y estado (activo) del cliente.</li>
     *   <li>Verificar idempotencia (no existe otro pago no anulado para cliente+mes+año).</li>
     *   <li>Normalizar fecha de pago (por defecto hoy) y validar que no sea futura.</li>
     *   <li>Calcular fecha de expiración (mensual o por días personalizados).</li>
     *   <li>Persistir con estado {@code UP_TO_DATE}.</li>
     *   <li>Auditar creación.</li>
     * </ol>
     * @param dto datos del pago (cliente, monto, método, período, duración opcional)
     * @return pago creado en formato DTO
     * @throws IllegalArgumentException datos inválidos (monto <= 0, fecha futura, cliente inexistente, mes/año fuera de rango)
     * @throws IllegalStateException reglas de negocio incumplidas (cliente inactivo, pago duplicado)
     */
    public PaymentDTO registerPayment(PaymentDTO dto) {
        validateRegisterInput(dto);
        var client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (!client.isActive()) {
            throw new IllegalStateException("El cliente está inactivo");
        }
        if (paymentRepository.existsByClient_IdAndMonthAndYearAndVoidedFalse(dto.getClientId(), dto.getMonth(), dto.getYear())) {
            throw new IllegalStateException("Ya existe un pago válido para ese período");
        }

        LocalDate payDate = dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now();
        if (dto.getPaymentDate() != null && payDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser futura");
        }

        LocalDate expiration = computeExpiration(payDate, dto.getDurationDays());

        Payment payment = fromDTO(dto);
        payment.setClient(client);
        payment.setState(PaymentState.UP_TO_DATE);
        payment.setPaymentDate(payDate);
        payment.setExpirationDate(expiration);
        payment.setDurationDays(dto.getDurationDays());

        Payment saved = paymentRepository.save(payment);
        auditService.logPaymentCreation(saved);
        PaymentDTO out = toDTO(saved);
        out.setExpirationDate(expiration);
        out.setState(PaymentState.UP_TO_DATE);
        return out;
    }

    /**
     * Devuelve pagos paginados aplicando filtros opcionales.
     * @param clientId id de cliente (nullable)
     * @param from fecha mínima (paymentDate >= from)
     * @param to fecha máxima (paymentDate <= to)
     * @param state estado persistido (UP_TO_DATE, EXPIRED, VOIDED) o null para cualquiera
     * @param pageable configuración de paginación y orden
     * @return página de PaymentDTO
     */
    public Page<PaymentDTO> findPayments(Long clientId, LocalDate from, LocalDate to, PaymentState state, Pageable pageable) {
        return paymentRepository.findByFilters(clientId, from, to, state, pageable)
                .map(this::toDTO);
    }

    /**
     * Busca un pago por id y lo expone como DTO.
     * @param id identificador
     * @return Optional con DTO si existe
     */
    public Optional<PaymentDTO> findDTOById(Long id) {
        return paymentRepository.findById(id).map(this::toDTO);
    }

    /**
     * Anula (void) un pago vigente agregando trazabilidad (usuario administrador y motivo) y cambiando su estado a VOIDED.
     * @param id id del pago
     * @param reason motivo textual provisto por el usuario
     * @return DTO actualizado
     * @throws IllegalArgumentException si no existe
     * @throws IllegalStateException si ya estaba anulado
     */
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

    /**
     * Obtiene pagos que vencerán exactamente dentro de 3 días (configurable fuera de este método) y que aún están UP_TO_DATE.
     * Útil para disparar recordatorios de renovación antes del vencimiento.
     * @return lista de entidades Payment ya que el proceso de emails puede necesitar datos del cliente (se hace fetch en repositorio específico para job de recordatorios)
     */
    public List<Payment> findExpiringPayments() {
        return paymentRepository.findByExpirationDateAndPaymentStateAndVoidedFalse(
                LocalDate.now().plusDays(3),
                PaymentState.UP_TO_DATE
        );
    }

    /**
     * Obtiene pagos ya vencidos (expirationDate < hoy) cuyo estado fue materializado como EXPIRED por el job.
     * @return lista de pagos expirados
     */
    public List<Payment> findOverduePayments() {
        return paymentRepository.findByExpirationDateBeforeAndPaymentState(
                LocalDate.now(),
                PaymentState.EXPIRED
        );
    }

    /**
     * Cuenta pagos cuyo estado persistido es EXPIRED y no están anulados.
     * @return cantidad de pagos expirados vigentes
     */
    public long countExpiredPayments() {
        return paymentRepository.countByPaymentStateAndVoidedFalse(PaymentState.EXPIRED);
    }

    /**
     * Marca en lote como EXPIRED todos los pagos UP_TO_DATE vencidos (expirationDate < hoy, voided = false).
     * Idempotente: ejecutarlo varias veces en el mismo día luego de la primera actualización devolverá 0.
     * <p>
     * Justificación: persiste el estado para reportes, integraciones (ej. control de acceso físico) y consultas eficientes por índice.
     * @return número de filas actualizadas
     */
    @Transactional
    public int expireOverduePayments() {
        return paymentRepository.bulkExpire(LocalDate.now(), PaymentState.UP_TO_DATE, PaymentState.EXPIRED);
    }

    /**
     * Calcula una fecha conceptual de vencimiento "base" para un período (ej: día 10 del mes o su máximo si el mes tiene menos días),
     * y se usa para inferir estados en períodos sin pago registrado.
     * @param month mes (1-12)
     * @param year año (>= 2000)
     * @return fecha de dueDate
     * @throws IllegalArgumentException si parámetros fuera de rango
     */
    public LocalDate computeDueDate(int month, int year) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        if (year < 2000) throw new IllegalArgumentException("El año debe ser >= 2000");
        YearMonth ym = YearMonth.of(year, month);
        int day = Math.min(10, ym.lengthOfMonth());
        return LocalDate.of(year, month, day);
    }

    /**
     * Determina el estado lógico de un período (mes/año) para un cliente aunque no exista un Payment para ese período.
     * Reglas simplificadas actuales:
     * <ul>
     *     <li>Existe pago no anulado del período → si venció y superó gracia → EXPIRED; si no → UP_TO_DATE.</li>
     *     <li>No existe pago → si venció y superó gracia → EXPIRED; si no → UP_TO_DATE ("aún dentro de ventana para pagar").</li>
     * </ul>
     * Nota: este método no consulta ni fuerza materialización de EXPIRED; se usa sobre todo para vistas rápidas o filtros de clientes.
     * @param clientId id de cliente
     * @param month mes
     * @param year año
     * @return estado lógico estimado
     */
    public PaymentState computePeriodState(Long clientId, int month, int year) {
        if (clientId == null) throw new IllegalArgumentException("clientId es obligatorio");
        if (!clientRepository.existsById(clientId)) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        LocalDate due = computeDueDate(month, year);
        LocalDate graceEnd = due.plusDays(GRACE_DAYS);
        LocalDate today = LocalDate.now();
        if (today.isAfter(graceEnd)) {
            return PaymentState.EXPIRED;
        }
        return PaymentState.UP_TO_DATE;
    }

    // ===================== Métodos internos de apoyo =====================

    private void validateRegisterInput(PaymentDTO dto) {
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
    }

    private LocalDate computeExpiration(LocalDate paymentDate, Integer durationDays) {
        if (durationDays != null) {
            if (durationDays < 1) throw new IllegalArgumentException("La duración debe ser al menos 1 día");
            return paymentDate.plusDays(durationDays);
        }
        return paymentDate.plusMonths(1);
    }

    private PaymentDTO toDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setClientId(payment.getClient().getId());
        dto.setClientFirstName(payment.getClient().getFirstName());
        dto.setClientLastName(payment.getClient().getLastName());
        dto.setClientEmail(payment.getClient().getEmail());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setMonth(payment.getMonth());
        dto.setYear(payment.getYear());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setExpirationDate(payment.getExpirationDate());
        dto.setDurationDays(payment.getDurationDays());
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
        payment.setDurationDays(dto.getDurationDays());
        return payment;
    }
}
