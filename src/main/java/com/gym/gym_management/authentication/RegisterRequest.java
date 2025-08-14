package com.gym.gym_management.authentication;
/**
 * DTO (Data Transfer Object) que representa la información enviada por el cliente
 * para registrar un nuevo usuario en el sistema.
 *
 * Contiene:
 * - Email: será el nombre de usuario en el sistema.
 * - Password: la contraseña que será encriptada antes de guardarse.
 * - Role: el rol asignado al usuario (por ejemplo, USER o CLIENT).
 *
 * Relación con los requerimientos:
 * Este objeto forma parte del flujo de registro implementado en AuthenticationService,
 * cumpliendo con la funcionalidad de creación de usuarios protegida por roles.
 * En el caso del sistema, solo los administradores (rol USER) pueden registrar nuevos usuarios.
 */

public class RegisterRequest {
    private String email;
    // Contraseña del usuario (se enviará en texto plano desde el cliente,
    // pero será cifrada en el backend antes de almacenarse).
    private String password;
    private String role;

    // === CONSTRUCTORES ===
    //Constructor vacío requerido por frameworks como Spring
    // para poder deserializar automáticamente datos JSON a este objeto.
    public RegisterRequest() {

    }

    public RegisterRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
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

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}
