package com.gym.gym_management.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) que representa la información necesaria para registrar
 * un nuevo administrador en el sistema.
 *
 * Incluye validaciones para garantizar la calidad y seguridad de los datos:
 * - Email válido y obligatorio
 * - Contraseña con requisitos mínimos de seguridad
 *
 * Este DTO está diseñado específicamente para la creación de usuarios con rol ADMIN
 * por parte de administradores ya existentes en el sistema.
 */
public class AdminRegisterDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    /**
     * Contraseña que debe cumplir con requisitos de seguridad:
     * - Al menos 8 caracteres
     * - Al menos una letra mayúscula
     * - Al menos una letra minúscula
     * - Al menos un número
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
        message = "La contraseña debe contener al menos una letra mayúscula, una minúscula y un número"
    )
    private String password;

    /**
     * Campo para confirmar que la contraseña fue escrita correctamente.
     * La validación de coincidencia se realizará en el servicio.
     */
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmPassword;

    // === CONSTRUCTORES ===

    /**
     * Constructor vacío requerido para la deserialización de JSON
     */
    public AdminRegisterDTO() {
    }

    /**
     * Constructor con todos los campos
     */
    public AdminRegisterDTO(String email, String password, String confirmPassword) {
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    // === GETTERS Y SETTERS ===

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
