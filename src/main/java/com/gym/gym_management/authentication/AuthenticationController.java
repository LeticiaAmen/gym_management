package com.gym.gym_management.authentication;


import com.gym.gym_management.configuration.JwtService;
import com.gym.gym_management.service.RateLimitService;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controlador REST encargado de manejar las operaciones de autenticación y registro de usuarios.
 * Expone endpoints REST para iniciar sesión (login) y registrar nuevos usuarios.
 *
 * Funcionalidad:
 * - /auth/login → Autenticación de usuarios y generación de token JWT.
 * - /auth/admin/register → Registro específico de nuevos administradores (solo accesible para administradores).
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
    private final RateLimitService rateLimitService; // Nuevo servicio para rate limiting

    //Constructor con inyección de dependencias
    @Autowired
    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService, AuthenticationManager authenticationManager, RateLimitService rateLimitService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.rateLimitService = rateLimitService;
    }


    /**
     * Endpoint para registrar un nuevo usuario.
     * Solo accesible por usuarios con rol "USER" (que en este sistema actúan como administradores).
     *
     * @param request objeto con datos del usuario a registrar (nombre, email, contraseña, rol, etc.)
     * @return respuesta con los datos de autenticación (token JWT) del usuario registrado.
     */
//    @PreAuthorize("hasRole('USER')")
//    @PostMapping("/register")
//    public ResponseEntity<AuthenticationResponse> register (
//            @RequestBody RegisterRequest request) {
//        return ResponseEntity.ok(authenticationService.register(request));
//    }

    /**
     * Endpoint específico para registrar un nuevo administrador.
     * Solo accesible por usuarios con rol ADMIN.
     * Incluye validación de campos del DTO.
     *
     * @param adminDTO objeto con datos del administrador a registrar (email y contraseña)
     * @return mensaje de éxito o error según el resultado de la operación
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody AdminRegisterDTO adminDTO) {
        try {
            // Verificar que las contraseñas coincidan
            if (!adminDTO.getPassword().equals(adminDTO.getConfirmPassword())) {
                return ResponseEntity.badRequest().body("Las contraseñas no coinciden");
            }

            // Registrar el nuevo administrador
            authenticationService.registerAdmin(adminDTO);
            return ResponseEntity.ok("Administrador registrado exitosamente");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
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
    public ResponseEntity<AuthenticationResponse> login (@RequestBody(required = false) AuthenticationRequest request, HttpServletRequest httpRequest) {
        String email = (request != null ? request.getEmail() : null);
        String ip = httpRequest.getRemoteAddr();
        // Verificar si está permitido intentar (bloque previo)
        rateLimitService.assertLoginAllowed(email, ip);
        try{
            if (request == null || email == null || request.getPassword() == null
                    || email.isBlank() || request.getPassword().isBlank()) {
                // Registrar fallo (entrada inválida también cuenta para evitar enumeración)
                rateLimitService.registerLoginFailure(email, ip);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            //Autenticación de usuario con las credenciales proporcionadas
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
            // Éxito: limpiar contador
            rateLimitService.registerLoginSuccess(email, ip);
            //Generar token jwt si las credenciales son válidas
            String token = jwtService.generateToken(authentication);
            return ResponseEntity.ok(new AuthenticationResponse(token));
        } catch (AuthenticationException ex){
            // Fallo de credenciales: registrar intento fallido
            rateLimitService.registerLoginFailure(email, ip);
            //si las credenciales son incorrectas, devuelve HTTP 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
