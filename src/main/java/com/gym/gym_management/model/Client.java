package com.gym.gym_management.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private boolean isActive = true;

    private String notes;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Campos para pausa/reanudar suscripción
    private LocalDate pausedFrom;
    private LocalDate pausedTo;
    private String pauseReason;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructor vacío requerido por JPA
    public Client() {}

    // Constructor con campos obligatorios
    public Client(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.startDate = LocalDate.now();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDate getPausedFrom() { return pausedFrom; }
    public void setPausedFrom(LocalDate pausedFrom) { this.pausedFrom = pausedFrom; }

    public LocalDate getPausedTo() { return pausedTo; }
    public void setPausedTo(LocalDate pausedTo) { this.pausedTo = pausedTo; }

    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    // Métodos de conveniencia para la relación bidireccional con Payment
    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setClient(this);
    }

    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setClient(null);
    }
}
