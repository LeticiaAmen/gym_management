package com.gym.gym_management.repository;

import com.gym.gym_management.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de logs de notificaciones.
 * <p>
 * Proporciona métodos para consultar y gestionar el historial de notificaciones
 * enviadas a los clientes del gimnasio.
 *
 * @author GymManagement
 * @version 1.0
 */
@Repository
public interface INotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * Busca si ya existe una notificación enviada para un pago específico.
     * <p>
     * Este método es útil para evitar enviar recordatorios duplicados al mismo cliente
     * para el mismo pago.
     *
     * @param paymentId ID del pago
     * @param notificationType tipo de notificación
     * @return Optional con el log de notificación si existe
     */
    Optional<NotificationLog> findByPaymentIdAndNotificationType(Long paymentId,
                                                                NotificationLog.NotificationType notificationType);

    /**
     * Obtiene todas las notificaciones enviadas para un pago específico.
     * <p>
     * Útil para mostrar el historial completo de recordatorios enviados a un cliente.
     *
     * @param paymentId ID del pago
     * @return lista de notificaciones ordenadas por fecha de envío descendente
     */
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.paymentId = :paymentId ORDER BY nl.sentDate DESC")
    List<NotificationLog> findByPaymentIdOrderBySentDateDesc(@Param("paymentId") Long paymentId);

    /**
     * Verifica si se ha enviado algún recordatorio para una lista de pagos.
     * <p>
     * Este método optimizado permite consultar múltiples pagos de una vez
     * para mostrar el estado en reportes.
     *
     * @param paymentIds lista de IDs de pagos
     * @return lista de logs de notificación para los pagos consultados
     */
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.paymentId IN :paymentIds AND nl.status = 'SENT'")
    List<NotificationLog> findSentNotificationsByPaymentIds(@Param("paymentIds") List<Long> paymentIds);
}
