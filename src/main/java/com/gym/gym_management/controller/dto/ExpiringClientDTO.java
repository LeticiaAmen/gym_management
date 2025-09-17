package com.gym.gym_management.controller.dto;

import java.time.LocalDate;

/**
 * Representa un cliente cuyo último pago válido vence en los próximos 7 días.
 * Incluye información mínima y la fecha de expiración para mostrar en reportes.
 */
public class ExpiringClientDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean active;
    private LocalDate expirationDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
}

