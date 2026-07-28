package com.gym.gym_management.controller.dto;

import com.gym.gym_management.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO seguro de usuario: deliberadamente no contiene la contraseña. */
public class UserDTO {
    private Long id;

    // @NotBlank y @Email validan el dato recibido antes de usarlo.
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    private Role role;

    // No exponemos el password en el DTO

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
