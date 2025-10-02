package com.gym.gym_management.service;

import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.model.*;
import com.gym.gym_management.repository.IPaymentRepository;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IUserRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
 *     <li><b>Persistencia de estado</b>: se guardan estados {@code UP_TO_DATE}, {@code EXPIRED}, {@code VOIDED}. El estado EXPIRED se materializa mediante un job diario (bulk update) apenas la fecha de expiración queda en el pasado.</li>
 *     <li><b>Anulación (void)</b>: marca lógica que preserva trazabilidad y evita borrar registros históricos.</li>
 *     <li><b>Consultas filtradas</b>: soporte de paginado y filtros por cliente, fechas y estado.</li>
 *     <li><b>Recordatorios</b>: obtención de pagos próximos a vencer (para disparar emails).</li>
 *     <li><b>Expiración masiva</b>: transición automática UP_TO_DATE -> EXPIRED cuando expiran (job llama {@link #expireOverduePayments()}).</li>
 * </ul>
 * Diseño y decisiones:
 * <ul>
 *     <li>Se eliminan días de gracia: un pago pasa a EXPIRED en cuanto su expirationDate es menor a hoy (el job lo materializa).</li>
 *     <li>Para períodos sin pago, el estado derivado es EXPIRED si la fecha conceptual (día 10) está en el pasado; de lo contrario UP_TO_DATE.</li>
 * </ul>
 */
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
     * Recupera pagos aplicando filtros dinámicos (cliente, rango de fechas y estado) usando Specifications.
     * <p>
     * Por qué Specifications:
     * <ul>
     *   <li>Evitan construir manualmente una consulta JPQL con muchos OR/AND y parámetros opcionales.</li>
     *   <li>Permiten componer predicados de forma segura (si un filtro es null simplemente no se agrega).</li>
     *   <li>Solucionan el error 500 que ocurría cuando la query con parámetros opcionales (from / to) chocaba con la sintaxis.</li>
     *   <li>Facilitan ampliar en el futuro (ej: filtrar por método de pago) sin reescribir la consulta completa.</li>
     * </ul>
     * Flujo interno:
     * <ol>
     *   <li>Comienza con un Specification vacío.</li>
     *   <li>Si viene clientId agrega predicate: client.id = :clientId.</li>
     *   <li>Si viene from agrega predicate: paymentDate >= :from.</li>
     *   <li>Si viene to agrega predicate: paymentDate <= :to.</li>
     *   <li>Si viene state agrega predicate: paymentState = :state.</li>
     *   <li>Ejecuta findAll(spec, pageable) y mapea a DTO.</li>
     * </ol>
     * @param clientId id del cliente (opcional)
     * @param queryText texto de búsqueda para nombre, apellido o email del cliente (opcional)
     * @param from fecha mínima inclusive (paymentDate >= from) – null ignora filtro
     * @param to fecha máxima inclusive (paymentDate <= to) – null ignora filtro
     * @param state estado persistido (UP_TO_DATE, EXPIRED, VOIDED) o null para cualquiera
     * @param pageable paginación y orden
     * @return página de pagos en formato DTO
     */
    public Page<PaymentDTO> findPayments(Long clientId, String queryText, LocalDate from, LocalDate to, PaymentState state, Pageable pageable) {
        Specification<Payment> spec = Specification.where((root, query, cb) -> {
            // fetch join para evitar N+1 cuando luego accedemos a client.* en toDTO
            root.fetch("client", JoinType.LEFT);
            query.distinct(true);
            return cb.conjunction();
        });
        if (clientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("client").get("id"), clientId));
        }
        if (queryText != null && !queryText.isBlank()) {
            String pattern = "%" + queryText.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                var client = root.get("client");
                return cb.or(
                        cb.like(cb.lower(client.get("firstName")), pattern),
                        cb.like(cb.lower(client.get("lastName")), pattern),
                        cb.like(cb.lower(client.get("email")), pattern)
                );
            });
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("paymentDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("paymentDate"), to));
        }
        if (state != null) {
            LocalDate today = LocalDate.now();
            if (state == PaymentState.EXPIRED) {
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.equal(root.get("paymentState"), PaymentState.EXPIRED),
                        cb.and(
                                cb.equal(root.get("paymentState"), PaymentState.UP_TO_DATE),
                                cb.lessThan(root.get("expirationDate"), today)
                        )
                ));
            } else if (state == PaymentState.UP_TO_DATE) {
                spec = spec.and((root, query, cb) -> cb.and(
                        cb.equal(root.get("paymentState"), PaymentState.UP_TO_DATE),
                        cb.greaterThanOrEqualTo(root.get("expirationDate"), today)
                ));
            } else if (state == PaymentState.VOIDED) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentState"), PaymentState.VOIDED));
            } else {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentState"), state));
            }
        }
        return paymentRepository.findAll(spec, pageable).map(this::toDTO);
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
     * Determina el estado para un período mes/año:
     * <ul>
     *   <li>Si existe un Payment no anulado se devuelve su estado persistido (UP_TO_DATE o EXPIRED o VOIDED).</li>
     *   <li>Si no existe Payment: se computa la fecha conceptual (día 10). Si está en el pasado ( < hoy ) => EXPIRED; si hoy es igual o anterior => UP_TO_DATE.</li>
     * </ul>
     * No hay días de gracia: la comparación es directa contra hoy.
     * @param clientId id cliente
     * @param month mes (1-12)
     * @param year año (>=2000)
     * @return estado resultante
     */
    public PaymentState computePeriodState(Long clientId, int month, int year) {
        if (clientId == null) throw new IllegalArgumentException("clientId es obligatorio");
        if (!clientRepository.existsById(clientId)) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        return paymentRepository.findByClient_IdAndMonthAndYearAndVoidedFalse(clientId, month, year)
                .map(Payment::getState)
                .orElseGet(() -> {
                    LocalDate due = computeDueDate(month, year);
                    LocalDate today = LocalDate.now();
                    return today.isAfter(due) ? PaymentState.EXPIRED : PaymentState.UP_TO_DATE;
                });
    }

    /**
     * Deriva el estado actual de la membresía de un cliente considerando su último pago no anulado.
     * <p>
     * Regla simple (alineada a la necesidad de filtrar clientes por estado):
     * <ul>
     *   <li>Si no tiene pagos válidos registrados -> EXPIRED (se considera vencido).</li>
     *   <li>Si el último pago (expirationDate) es anterior a hoy -> EXPIRED.</li>
     *   <li>Si la fecha de expiración es hoy o futura -> UP_TO_DATE.</li>
     * </ul>
     * Pagos VOIDED no participan porque el repositorio busca voided=false.
     * @param clientId id de cliente
     * @return estado derivado (UP_TO_DATE o EXPIRED)
     * @throws IllegalArgumentException si clientId es null
     */
    public PaymentState deriveCurrentMembershipState(Long clientId) {
        if (clientId == null) throw new IllegalArgumentException("clientId es obligatorio");
        Payment last = paymentRepository.findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(clientId);
        LocalDate today = LocalDate.now();
        if (last == null) {
            return PaymentState.EXPIRED; // sin pagos se asume vencido
        }
        LocalDate exp = last.getExpirationDate();
        if (exp != null && exp.isBefore(today)) {
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
        // Estado efectivo: si está marcado VOIDED se respeta; si está UP_TO_DATE pero expirationDate < hoy => EXPIRED lógico
        PaymentState persisted = payment.getState();
        PaymentState effective = persisted;
        if (persisted == PaymentState.UP_TO_DATE && payment.getExpirationDate() != null && payment.getExpirationDate().isBefore(LocalDate.now())) {
            effective = PaymentState.EXPIRED;
        }
        dto.setState(effective);
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
