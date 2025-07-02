package com.gym.gym_management.authentication;

public class AuthenticationRequest {
    private String email;
    private String password;

    // === CONSTRUCTORES ===
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
