package com.gym.gym_management.service;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentState;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    // Repositorio de clientes para vincular pagos a un cliente existente
    @Autowired
    private IClientRepository clientRepository;

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
     * Registra un pago asociado a un cliente.
     *
     *  @param clientId id del cliente que realiza el pago
     * @param paymentDate fecha en que se registra el pago (puede ser actual o proporcionada en el request)
     * @param expirationDate fecha de vencimiento calculada en base a la duración
     * @param amount monto del pago
     * @return el pago registrado en la base de datos
     * @throws Exception si el cliente no existe en la base de datos
     */
    public Payment registerPayment(Long clientId,
                                   LocalDate paymentDate,
                                   LocalDate expirationDate,
                                   Double amount) throws Exception {

        // Se busca el cliente en la base de datos con su ID
        // Si no se encuentra, se lanza una excepción
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new Exception("No se encontró el cliente con id: " + clientId));

        // Se valida que el cliente esté activo.
        // Si está inactivo, se lanza una IllegalStateException
        //porque no se debe permitir registrar pagos es este estado.
        if (!client.isActive()){
            throw new IllegalStateException("El cliente está inactivo y no puede registrar pagos");
        }

        // Se crea un nuevo objeto Payment y se setean los datos
        // todo: contemplar escenarios donde el cliente tenga más de un pago por hacer, y aunque haga uno su estado no debería ser UP_TO_DATE
        Payment payment = new Payment();
        payment.setPaymentDate(paymentDate);
        payment.setExpirationDate(expirationDate);
        payment.setAmount(amount);
        payment.setPaymentState(PaymentState.UP_TO_DATE);
        // Se asocia el pago al cliente (esto asegura que el cliente tenga el historial completo).
        client.registerPayment(payment);
        // Se guarda el pago en la base de datos y se retorna la entidad persistida.
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
