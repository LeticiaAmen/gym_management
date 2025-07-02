package com.gym.gym_management.authentication;

public class AuthenticationResponse {
    private String token;

    // === CONSTRUCTORES ===
    public AuthenticationResponse() {

    }

    public AuthenticationResponse(String token) {
        this.token = token;
    }

    // === GETTERS Y SETTERS ===
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // === BUILDER MANUAL ===
    public static AuthenticationResponseBuilder builder() {
        return new AuthenticationResponseBuilder();
    }

    public static class AuthenticationResponseBuilder {
        private String token;

        public AuthenticationResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthenticationResponse build() {
            return new AuthenticationResponse(token);
        }
    }
}
