package com.gym.gym_management.authentication;


import com.gym.gym_management.configuration.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Controlador REST encargado de manejar las operaciones de autenticación y registro de usuarios.
 * Expone endpoints REST para iniciar sesión (login) y registrar nuevos usuarios.
 *
 * Funcionalidad:
 * - /auth/register → Registro de nuevos usuarios (solo accesible para administradores o rol USER).
 * - /auth/login → Autenticación de usuarios y generación de token JWT.
 *
 * Esta clase interactúa con:
 * - AuthenticationService → Contiene la lógica de registro de usuarios.
 * - JwtService → Genera el token JWT para las sesiones.
 * - AuthenticationManager → Verifica credenciales de inicio de sesión.
 *
 * Relación con los requerimientos:
 * Cumple con la parte de "Autenticación y Seguridad" del proyecto,
 * implementando autenticación con JWT y restricción de acceso por roles.
 */

@RestController
@RequestMapping("/auth")
//ruta base para los endpoints de autenticación
public class AuthenticationController {

    // Servicio que gestiona el registro y la lógica de autenticación.
    private final AuthenticationService authenticationService;
    // Servicio encargado de generar y validar tokens JWT.
    private final JwtService jwtService;
    // Administrador de autenticaciones que valida usuario y contraseña.
    private final AuthenticationManager authenticationManager;

    //Constructor con inyección de dependencias
    @Autowired
    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }


    /**
     * Endpoint para registrar un nuevo usuario.
     * Solo accesible por usuarios con rol "USER" (que en este sistema actúan como administradores).
     *
     * @param request objeto con datos del usuario a registrar (nombre, email, contraseña, rol, etc.)
     * @return respuesta con los datos de autenticación (token JWT) del usuario registrado.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register (
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    /**
     * Endpoint para iniciar sesión (login).
     * Recibe email y contraseña, los valida y, si son correctos,
     * genera un token JWT que el cliente usará para autenticarse en futuras peticiones.
     *
     * @param request objeto con email y contraseña del usuario (opcional para evitar 400 si no se envía body).
     * @return token JWT en caso de éxito, o estado UNAUTHORIZED si las credenciales son inválidas o faltan.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login (@RequestBody(required = false) AuthenticationRequest request) {
        try{
            // Validación temprana: si no hay body o faltan credenciales, devolver 401
            if (request == null || request.getEmail() == null || request.getPassword() == null
                    || request.getEmail().isBlank() || request.getPassword().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            //Autenticación de usuario con las credenciales proporcionadas
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            //Generar token jwt si las credenciales son válidas
            String token = jwtService.generateToken(authentication);
            return ResponseEntity.ok(new AuthenticationResponse(token));
        } catch (AuthenticationException ex){
            //si las credenciales son incorrectas, devuelve HTTP 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
