package com.gym.gym_management.repository;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para acceder y consultar pagos (Payment) persistidos.
 * <p>
 * Responsabilidades principales:
 * <ul>
 *   <li>Operaciones CRUD básicas (heredadas de {@link JpaRepository}).</li>
 *   <li>Consultas derivadas por convención (findBy...) para casos frecuentes y eficientes.</li>
 *   <li>Consultas personalizadas con JPQL (@Query) para escenarios específicos
 *   (recordatorios, expiraciones, filtros).</li>
 *   <li>Soporte de Specifications ({@link JpaSpecificationExecutor})
 *   para filtrado dinámico avanzado
 *   (usado actualmente en el servicio al buscar por fechas / estado).</li>
 * </ul>
 * <p>
 * Uso de Page / Pageable:<br>
 * Algunos métodos (como {@link #findByFilters(Long, LocalDate, LocalDate,
 * PaymentState, Pageable)}) devuelven un {@link Page} que encapsula:
 * <ul>
 *   <li>La lista parcial (segmento) de resultados solicitados según paginación.</li>
 *   <li>Metadatos: total de elementos, total de páginas, número de página actual,
 *   tamaño, etc.</li>
 * </ul>
 * {@link Pageable} permite al consumidor (capa web) indicar tamaño de página,
 * número y orden (sorting) sin reescribir la consulta.
 * Esto evita traer todo a memoria y mejora el rendimiento con
 * grandes volúmenes históricos de pagos.
 */
@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    /**
     * Obtiene todos los pagos de un cliente (sin filtrar por estado o período).
     * Útil para historiales completos.
     * @param clientId id del cliente
     * @return lista de pagos (puede estar vacía)
     */
    List<Payment> findByClientId(Long clientId);

    /**
     * Pagos cuya fecha de expiración coincide exactamente con la fecha indicada
     * y aún están en estado dado.
     * Usado en el job de recordatorios
     * (ej: enviar aviso 3 días antes de vencer) filtrando también voided=false.
     * @param date fecha objetivo
     * @param paymentState estado (UP_TO_DATE típico para recordar antes de vencer)
     */
    List<Payment> findByExpirationDateAndPaymentStateAndVoidedFalse(LocalDate date, PaymentState paymentState);

    /**
     * Pagos ya expirados (expirationDate < date)
     * cuyo estado persistido coincide (normalmente EXPIRED).
     * Soporta reportes o dashboards de vencidos históricos.
     * @param date referencia (normalmente hoy)
     * @param paymentState estado esperado (EXPIRED)
     */
    List<Payment> findByExpirationDateBeforeAndPaymentState(LocalDate date, PaymentState paymentState);

    /**
     * Pagos realizados en un rango de fechas (inclusive) y no anulados.
     * Útil para reportes de caja / ingresos acotados.
     * @param from fecha mínima inclusive
     * @param to fecha máxima inclusive
     */
    List<Payment> findByPaymentDateBetweenAndVoidedFalse(LocalDate from, LocalDate to);

    /**
     * Consulta JPQL con filtros opcionales
     * (cliente, rango de fechas por paymentDate y estado persistido).
     * <p>
     * NOTA: Actualmente el servicio principal usa Specifications
     * (findAll(spec,...)) para mayor flexibilidad.
     * Este método se mantiene como alternativa/legado o para casos donde se quiera una sola
     * consulta JPQL estática.
     * @param clientId id del cliente (null = cualquiera)
     * @param dateFrom fecha mínima paymentDate (null = sin filtro inferior)
     * @param dateTo fecha máxima paymentDate (null = sin filtro superior)
     * @param state estado persistido (null = cualquiera)
     * @param pageable paginación y orden
     * @return página de pagos coincidentes
     */
    @Query("SELECT p FROM Payment p WHERE (:clientId IS NULL OR p.client.id = :clientId) " +
           "AND (:dateFrom IS NULL OR p.paymentDate >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.paymentDate <= :dateTo) " +
           "AND (:state IS NULL OR p.paymentState = :state)")
    Page<Payment> findByFilters(
            @Param("clientId") Long clientId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("state") PaymentState state,
            Pageable pageable
    );

    /**
     * Verifica existencia de un pago válido (no anulado) para un cliente y período (mes/año)
     * evitando duplicados.
     */
    boolean existsByClient_IdAndMonthAndYearAndVoidedFalse(Long clientId, Integer month, Integer year);

    /**
     * Obtiene un pago específico (si existe y no está anulado) para un cliente y período.
     * Se usa para reflejar el estado persistido real (UP_TO_DATE, EXPIRED, VOIDED)
     * sin crear consultas duplicadas.
     */
    Optional<Payment> findByClient_IdAndMonthAndYearAndVoidedFalse(Long clientId, Integer month, Integer year);

    /**
     * Último pago (por fecha de expiración) válido de un cliente,
     * útil para determinar si su membresía sigue vigente.
     */
    Payment findTopByClient_IdAndVoidedFalseOrderByExpirationDateDesc(Long clientId);

    /**
     * Último pago efectivo (ordenando por expirationDate si existe o paymentDate como fallback).
     * JPQL con ORDER BY COALESCE para soportar pagos sin expirationDate explícita (ej: inicial antes de persistir lógica completa).
     */
    @Query("SELECT p FROM Payment p WHERE p.client.id = :clientId AND p.voided = false ORDER BY COALESCE(p.expirationDate, p.paymentDate) DESC")
    Payment findLastEffectiveByClient(@Param("clientId") Long clientId);

    /**
     * Cuenta pagos por estado (excluyendo anulados) para métricas rápidas (dashboard).
     */
    long countByPaymentStateAndVoidedFalse(PaymentState paymentState);

    /**
     * Pagos posteriores a una fecha (orden descendente) sin anular. Apoya listados cronológicos recientes.
     */
    List<Payment> findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(LocalDate date);

    /**
     * Pagos cuya expiración cae en un rango (ordenados ascendentemente) para generar recordatorios / reportes.
     */
    List<Payment> findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Pagos que vencen en una fecha exacta incluyendo datos del cliente (fetch join) optimizando el job de emails.
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.client c WHERE p.expirationDate = :date AND p.voided = false AND p.paymentState = 'UP_TO_DATE'")
    List<Payment> findByExpirationDateWithClient(@Param("date") LocalDate date);

    /**
     * Actualiza en bloque a EXPIRED todos los pagos que ya pasaron su fecha de expiración.
     * Mejora rendimiento frente a iterar registro a registro y asegura consistencia diaria.
     * @param today fecha de corte (normalmente LocalDate.now())
     * @param upToDate estado origen esperado
     * @param expired estado destino
     * @return cantidad de filas afectadas
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Payment p SET p.paymentState=:expired WHERE p.paymentState=:upToDate AND p.voided=false AND p.expirationDate < :today")
    int bulkExpire(@Param("today") LocalDate today,
                   @Param("upToDate") PaymentState upToDate,
                   @Param("expired") PaymentState expired);
}
