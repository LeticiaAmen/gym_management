package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador REST para gestionar pagos de clientes.
 * <p>
 * Expone endpoints administrativos (rol ADMIN) para:
 * <ul>
 *     <li>Registrar nuevos pagos aplicando reglas del dominio.</li>
 *     <li>Listar pagos con filtros (cliente, fechas, estado) y paginación.</li>
 *     <li>Anular pagos (void) manteniendo trazabilidad.</li>
 *     <li>Consultar un pago puntual.</li>
 *     <li>Consultar el estado dinámico de un período (sin crear registros). Estados disponibles: UP_TO_DATE, EXPIRED, VOIDED.</li>
 * </ul>
 * Siempre trabaja con DTOs para desacoplar la capa web de las entidades JPA.
 */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * Registra un nuevo pago asociado a un cliente.
     * @param request datos del pago (cliente, monto, período, método).
     * @return 201 + PaymentDTO creado o error acorde.
     */
    @PostMapping
    public ResponseEntity<?> registerPayment(@Valid @RequestBody PaymentDTO request) {
        try {
            PaymentDTO created = paymentService.registerPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("cliente no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }
            return ResponseEntity.badRequest().body(msg);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Lista pagos aplicando filtros opcionales.
     * Estados aceptados: UP_TO_DATE, EXPIRED, VOIDED.
     * @param clientId id de cliente (opcional).
     * @param from fecha mínima de paymentDate.
     * @param to fecha máxima de paymentDate.
     * @param state estado textual (UP_TO_DATE | EXPIRED | VOIDED).
     * @param pageable parámetros de paginación.
     */
    @GetMapping
    public ResponseEntity<?> getPayments(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "q") String queryText,
            @PageableDefault(sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest().body("El parámetro 'from' no puede ser posterior a 'to'");
        }
        log.debug("[GET /api/payments] params clientId={}, q='{}', from={}, to={}, rawState='{}', page={}, size={}", clientId, queryText, from, to, state, pageable.getPageNumber(), pageable.getPageSize());
        PaymentState parsedState = mapFlexibleState(state); // null si no viene / no se reconoce
        if (state != null) {
            log.debug("[GET /api/payments] parsedState={}", parsedState);
        }
        Page<PaymentDTO> page = paymentService.findPayments(clientId, queryText, from, to, parsedState, pageable);
        log.debug("[GET /api/payments] result elements={}, pageContentSize={}", page.getTotalElements(), page.getContent().size());
        return ResponseEntity.ok(page);
    }

    /**
     * Anula un pago existente marcándolo como VOIDED.
     * @param id identificador del pago.
     * @param reason motivo de anulación.
     */
    @PostMapping("/{id}/void")
    public ResponseEntity<?> voidPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            PaymentDTO dto = paymentService.voidPayment(id, reason);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) { // no encontrado
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) { // ya anulado
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Recupera un pago por id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> findById(@PathVariable Long id){
        Optional<PaymentDTO> payment = paymentService.findDTOById(id);
        return payment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Calcula el estado de un período sin necesidad de crear un pago.
     * Lógica derivada: si existe pago => UP_TO_DATE; si no y venció (venc+gracia) => EXPIRED; caso contrario UP_TO_DATE.
     */
    @GetMapping("/state")
    public ResponseEntity<?> getPeriodState(
            @RequestParam Long clientId,
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        try {
            PaymentState state = paymentService.computePeriodState(clientId, month, year);
            LocalDate due = paymentService.computeDueDate(month, year);
            LocalDate graceEnd = due.plusDays(3);
            return ResponseEntity.ok(Map.of(
                    "state", state.name(),
                    "dueDate", due.toString(),
                    "graceEnd", graceEnd.toString()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private PaymentState mapFlexibleState(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Normalización agresiva: quitar acentos, espacios, guiones, dos puntos y prefijo "PAGO"
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // quitar diacríticos
                .toUpperCase(Locale.ROOT)
                .replace("PAGO:", "")
                .replace("PAGO", "")
                .replace("_", " ")
                .replace('-', ' ')
                .trim();
        // Unificar múltiples espacios
        normalized = normalized.replaceAll(" +", " ");
        if (normalized.isBlank()) return null;
        if (normalized.equals("TODOS") || normalized.equals("ALL")) return null;

        // Sinónimos UP_TO_DATE
        if (normalized.equals("AL DIA") || normalized.equals("AL DIA") || normalized.equals("ALDIA") || normalized.equals("AL DIA")) {
            return PaymentState.UP_TO_DATE;
        }
        if (normalized.equals("UP TO DATE") || normalized.equals("UPTODATE")) {
            return PaymentState.UP_TO_DATE;
        }
        // Sinónimos EXPIRED
        if (normalized.startsWith("VENCID")) {
            return PaymentState.EXPIRED;
        }
        if (normalized.equals("EXPIRADO") || normalized.equals("EXPIRADOS")) {
            return PaymentState.EXPIRED;
        }
        // Sinónimos VOIDED
        if (normalized.startsWith("ANULAD")) {
            return PaymentState.VOIDED;
        }
        if (normalized.equals("VOID") || normalized.equals("VOIDED")) {
            return PaymentState.VOIDED;
        }
        // Intentar parse directo contra enum estándar
        try {
            return PaymentState.valueOf(normalized.replace(" ", "_")); // por si llega "UP TO DATE"
        } catch (Exception ex) {
            return null; // desconocido -> sin filtro
        }
    }
}
