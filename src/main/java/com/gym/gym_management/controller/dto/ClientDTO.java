package com.gym.gym_management.controller.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ClientDTO {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @Pattern(regexp = "^[0-9-+\\s]*$", message = "El teléfono solo debe contener números, espacios, - y +")
    private String phone;

    private boolean active;
    private LocalDate startDate;

    @Size(max = 500, message = "Las notas no deben exceder los 500 caracteres")
    private String notes;

    // Campos de pausa
    private LocalDate pausedFrom;
    private LocalDate pausedTo;
    private String pauseReason;

    // Campos de auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getPausedFrom() { return pausedFrom; }
    public void setPausedFrom(LocalDate pausedFrom) { this.pausedFrom = pausedFrom; }

    public LocalDate getPausedTo() { return pausedTo; }
    public void setPausedTo(LocalDate pausedTo) { this.pausedTo = pausedTo; }

    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
