package com.gym.gym_management.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    private Long id; //No usamos @GeneratedValue porque se asigna a partir de usuario

    @OneToOne
    @MapsId //Le indica a JPA que use el id del usuario como id del cliente
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    private String telephone;
    private boolean isActive;

    //relación bidireccional con pagos
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    public Client() {
    }

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
    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setClient(null);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

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
