package com.gym.gym_management.authentication;

/**
 * DTO (Data Transfer Object) que representa la información necesaria
 * para que un usuario pueda iniciar sesión en la aplicación.
 *
 * Contiene el email (nombre de usuario) y la contraseña que el cliente
 * envía al endpoint de autenticación (/auth/login).
 *
 * Relación con los requerimientos:
 * Forma parte del proceso de autenticación definido en "Autenticación y Seguridad".
 * Este objeto es enviado desde el frontend (Postman, cliente web o móvil)
 * hacia el backend para validar credenciales y generar un token JWT.
 */

public class AuthenticationRequest {
    private String email;
    // Contraseña del usuario, enviada en texto plano desde el cliente
    // pero validada y protegida en el backend (nunca almacenada en texto plano).
    private String password;

    // === CONSTRUCTORES ===
    //vacio es requerido por spring para deserializar automáticamente datos JSON a este objeto
    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String email, String password) {
        this.email = email;
        this.password = password;
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
}
