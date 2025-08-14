package com.gym.gym_management.service;

import com.gym.gym_management.model.Payment;
import com.gym.gym_management.repository.IPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio que encapsula la lógica de negocio relacionada con pagos.
 *
 * Funciones principales:
 * - Consultar pagos por cliente.
 * - Registrar un nuevo pago.
 * - Consultar todos los pagos o buscar por ID.
 * - Eliminar un pago con validación previa de existencia.
 *
 * Relación con los requerimientos:
 * - "Historial de Pagos" (Clientes): permite consultar pagos asociados a un cliente.
 * - "Panel de Administrador": permite registrar y eliminar pagos.
 * - Soporta la funcionalidad de "Recordatorios Automáticos de Pago"
 *   al exponer datos de vencimiento a otras capas de la aplicación.
 */
@Service
public class PaymentService {
    // Repositorio para acceder a la información de pagos en la base de datos.
    @Autowired
    private IPaymentRepository paymentRepository;

    /**
     * Obtiene todos los pagos asociados a un cliente específico.
     *
     * @param clientId identificador del cliente.
     * @return lista de pagos de ese cliente.
     */
    public List<Payment> getPaymentsByClientId(Long clientId){
        return paymentRepository.findByClientId(clientId);
    }

    /**
     * Registra un nuevo pago en la base de datos.
     *
     * @param payment entidad Payment a registrar.
     * @return el pago registrado.
     */
    public Payment registerPayment(Payment payment){
        return paymentRepository.save(payment);
    }

    /**
     * Obtiene todos los pagos registrados.
     * @return lista de pagos.
     */
    public List<Payment> findAll(){
        return paymentRepository.findAll();
    }

    /**
     * Busca un pago por su identificador.
     *
     * @param id identificador del pago.
     * @return Optional con el pago si existe, vacío si no.
     */
    public Optional<Payment> findById(Long id){
        return paymentRepository.findById(id);
    }

    /**
     * Elimina un pago por su identificador.
     * Verifica si el pago existe antes de eliminarlo.
     *
     * @param id identificador del pago a eliminar.
     * @throws Exception si el pago no existe.
     */
    public void delete(Long id) throws Exception {
        Optional<Payment> payment = paymentRepository.findById(id);
        if(payment.isPresent()){
            paymentRepository.deleteById(id);
        }else {
            throw new Exception("No se pudo eliminar el pago con id: " + id);
        }
    }

}
