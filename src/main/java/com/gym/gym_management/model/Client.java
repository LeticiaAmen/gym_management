package com.gym.gym_management.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Entidad JPA que representa a un cliente del gimnasio.
 *
 * Características principales:
 * - Relación 1:1 con User: cada Client está vinculado a un User (email/credenciales/rol).
 * - El ID de Client es el mismo ID del User asociado (patrón "shared primary key" con @MapsId).
 * - Mantiene datos de perfil (firstName, lastName, telephone, activo/inactivo).
 * - Relación 1:N con Payment (pagos del cliente), con cascada y orphanRemoval.
 *
 * Relación con los requerimientos:
 * - "Usuarios y Roles": el cliente hereda credenciales/rol desde User.
 * - "Gestión de Clientes": permite crear, modificar, desactivar (isActive), y vincular pagos.
 * - "Historial de Pagos": la colección payments almacena los pagos del cliente.
 *
 * Notas:
 * - No se usa @GeneratedValue en id: el ID viene del User asociado (por @MapsId).
 * - Los métodos registerPayment/removePayment mantienen la bidireccionalidad con Payment.
 */
@Entity
@Table(name = "clients")
public class Client {

    // Identificador de Client. No se genera automáticamente
    // se toma del USER asociado gracias a MapsId(Clave primaria compartida)
    @Id
    private Long id; //No usamos @GeneratedValue porque se asigna a partir de usuario

    //Relación 1:1 con user
    @OneToOne
    @MapsId //Le indica a JPA que use el id del usuario como id del cliente
    @JoinColumn(name = "user_id") //define la FK física en la tabla "clients" apuntando a "users".
    private User user;

    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    private String telephone;
    private boolean isActive;

    /**
     * Relación bidireccional 1:N con Payment.
     * - mappedBy = "client": la FK vive en Payment (campo payment.client).
     * - cascade = ALL: al persistir/actualizar/eliminar Client se propaga a sus Payments.
     * - orphanRemoval = true: si un Payment se quita de la lista, se elimina de la BD.
     */
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    public Client() {
    }

    /**
     * Constructor conveniente para inicializar la entidad.
     * OJO: al usar @MapsId, al setear user también se definirá el id del Client.
     */
    public Client(User user, String firstName, String lastName, String telephone, boolean isActive, List<Payment> payments) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.telephone = telephone;
        this.isActive = isActive;
        this.payments = payments;
    }

    //Método para agregar un pago
    public void registerPayment(Payment payment) {
        payments.add(payment);
        payment.setClient(this);
    }

    //Método para eliminar pago
    // Con orphanRemoval=true, el Payment se eliminará de la BD si ya estaba persistido.
    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setClient(null);
    }


    public Long getId() {
        return id;
    }
//    /**
//     * En modelos con @MapsId, normalmente el id se deriva de user.
//     * Solo usar este setter si se controla cuidadosamente la coherencia con user.getId().
//     */
//    public void setId(Long id) {
//        this.id = id;
//    }

    public User getUser() {
        return user;
    }

    /**
     * Al setear el User, en escenarios típicos @MapsId hará que id = user.getId().
     * Asegurate de que el User tenga un id asignado (persistido) antes de persistir Client.
     */
    public void setUser(User user) {
        this.user = user;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * Marca si el cliente está activo o no.
     * Útil para "pausar suscripciones" según requerimientos.
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }
}
