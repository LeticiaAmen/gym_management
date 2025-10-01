package com.gym.gym_management.controller.dto;

import java.time.LocalDate;

/**
 * Cliente con información mínima para reportes de vencidos, incluyendo la fecha
 * de expiración de su último pago válido (no anulado) y si se le envió recordatorio.
 */
public class OverdueClientDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean active;
    private LocalDate expirationDate; // fecha de vencimiento del último pago válido
    private boolean reminderSent; // indica si se envió recordatorio de vencimiento

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

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
}
