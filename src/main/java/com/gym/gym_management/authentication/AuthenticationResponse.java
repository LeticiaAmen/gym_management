package com.gym.gym_management.authentication;

/**
 * DTO (Data Transfer Object) que representa la respuesta enviada al cliente
 * después de un inicio de sesión o registro exitoso.
 *
 * Contiene el token JWT que el usuario deberá usar en sus solicitudes
 * posteriores para acceder a recursos protegidos de la API.
 *
 * Relación con los requerimientos:
 * Cumple con la parte de "Autenticación y Seguridad" al proveer un
 * mecanismo para que el cliente reciba y use el token JWT.
 *
 * Ejemplo de uso en JSON (respuesta del backend):
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 */

public class AuthenticationResponse {
    //token JWT generado para el usuario autenticado
    private String token;

    // === CONSTRUCTORES ===
    //vacio requerido por spring para deserializar o inicializar automáticamente objetos
    public AuthenticationResponse() {

    }

    //Constructor que permite inicializar la respuesta con un token
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
    /**
     * Método estático para construir una AuthenticationResponse
     * usando el patrón Builder, útil para una creación más legible y fluida.
     */
    public static AuthenticationResponseBuilder builder() {
        return new AuthenticationResponseBuilder();
    }

    //Clase interna que implementa el patrón Builder para AuthenticationResponse.
    public static class AuthenticationResponseBuilder {
        private String token;

        /**
         * Asigna el token JWT en la construcción del objeto.
         * @param token valor del token.
         * @return la misma instancia del builder para encadenar llamadas.
         */
        public AuthenticationResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Crea una nueva instancia de AuthenticationResponse
         * con los valores establecidos en el builder.
         */
        public AuthenticationResponse build() {
            return new AuthenticationResponse(token);
        }
    }
}
