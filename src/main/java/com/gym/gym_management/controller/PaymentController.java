package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.PaymentDTO;
import com.gym.gym_management.controller.dto.RegisterPaymentRequest;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controlador REST para gestionar operaciones relacionadas con pagos.
 *
 * Endpoints:
 * - GET    /payments         → Lista todos los pagos.
 * - GET    /payments/{id}    → Busca un pago por su id.
 * - POST   /payments/client/{clientId} → Registra un nuevo pago para un cliente.
 * - DELETE /payments/{id}             → Elimina un pago por su id.
 *
 * Relación con los requerimientos:
 * - "Gestión de Clientes" y "Historial de Pagos":
 *   Permite registrar, consultar y eliminar pagos.
 * - "Panel de Administrador":
 *   Los administradores pueden gestionar pagos y vencimientos.
 * - "Accesibilidad Web":
 *   Expone endpoints REST para acceso desde web/móvil.
 */

@RestController
@RequestMapping("/payments")
public class PaymentController {

    // Servicio de dominio que contiene la lógica de negocio para pagos.
    @Autowired
    private PaymentService paymentService;


    /**
     * Obtiene la lista completa de pagos.
     * Acceso: autenticado (según configuración de seguridad).
     * @return lista de pagos registrados.
     */
    @GetMapping
    public List<PaymentDTO> findAll(){
        return paymentService.findAll()
                .stream()
                .map(PaymentDTO::fromEntity)
                .toList();
    }


    /**
     * Busca un pago por su identificador.
     * Acceso: autenticado.
     *
     * @param id identificador del pago.
     * @return ResponseEntity con el pago si existe, o 404 si no se encuentra.
     */
    @GetMapping("{id}")
    public ResponseEntity<PaymentDTO> findById(@PathVariable Long id){
        Optional<Payment> payment = paymentService.findById(id);
        if(payment.isPresent()){
            return ResponseEntity.ok(PaymentDTO.fromEntity(payment.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Registra un nuevo pago asociado a un cliente
     * Acceso: restringido a usuarios con rol USER (en este caso, los administradores).
     * Flujo:
     * - Se valida que el request contenga una duración válida (si falta → 400 Bad Request).
     * - Se toma la fecha de pago enviada o, en su defecto, la fecha actual.
     * - Se calcula la fecha de vencimiento sumando los días definidos en la duración.
     * - Se delega el registro del pago al servicio de pagos.
     *
     * @param clientId identificador del cliente al que se registra el pago
     * @param request objeto con los datos necesarios para el pago
     * @return el pago registrado en formato DTO
     */
    @PostMapping("/client/{clientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentDTO> save(@PathVariable Long clientId,
                        @RequestBody RegisterPaymentRequest request) {
        // validamos que la duración sea proporcionada para evitar nullpointers
        if(request.getDuration() == null) {
            // Si falta la duración, se responde con un 400 bad request
            return ResponseEntity.badRequest().build();
        }
        //Se toma la fecha de pago enviada o la actual si no se especifica
        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now();
        //calculamos la fecha de vencimiento sumando los días de la duración
        LocalDate expirationDate = paymentDate.plusDays(request.getDuration().getDays());

       try{
           // se le delega al servicio la lógica de registrar el pago
           Payment payment = paymentService.registerPayment(clientId, paymentDate, expirationDate, request.getAmount());
           // si sale bien, se convierte la entidad a un DTO  (para no exponer la entidad completa)
           // y se retorna una respuesta HTTP 200 OK con el objeto registrado.
           return ResponseEntity.ok(PaymentDTO.fromEntity(payment));

       }catch (IllegalStateException e ){
           // Este catch captura casos en los que el estado del sistema impide registrar el pago.
           // Ejemplo típico: el cliente no existe, está inactivo, o ya tiene un pago vigente.
           // Se responde con 403 Forbidden porque la acción no está permitida en ese contexto.
           return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

       }catch (Exception e){
           // Este catch es un "safety net" para cualquier otro error inesperado.
           // Se responde con 400 Bad Request indicando que la solicitud no se pudo procesar.
           // En un escenario real, aquí también se podría loggear el error para diagnóstico.
           return ResponseEntity.badRequest().build();
       }
    }

    /**
     * Elimina un pago existente por su id.
     * Acceso: Hay que restringir a rol USER para que solo los administradores puedan eliminar pagos.
     *
     * @param id identificador del pago a eliminar.
     * @return mensaje de confirmación.
     * @throws Exception si ocurre un error durante la eliminación.
     */
   @DeleteMapping("/{id}")
   @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> delete(@PathVariable Long id) throws Exception{
       paymentService.delete(id);
       return ResponseEntity.ok("Se eliminó el pago con id: " + id);


   }

}
