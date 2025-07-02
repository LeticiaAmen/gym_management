package com.gym.gym_management.authentication;


public class RegisterRequest {
    private String email;
    private String password;
    private String role;

    // === CONSTRUCTORES ===
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
