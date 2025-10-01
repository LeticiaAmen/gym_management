package com.gym.gym_management.authentication;

import com.gym.gym_management.configuration.JwtService;
import com.gym.gym_management.repository.IUserRepository;
import com.gym.gym_management.model.User;
import com.gym.gym_management.model.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de manejar la lógica de autenticación y registro de usuarios.
 *
 * Funcionalidades principales:
 * - Registrar nuevos administradores (solo por usuarios con rol ADMIN).
 * - Autenticar usuarios existentes y generar un token JWT para sesiones seguras.
 *
 * Relación con los requerimientos:
 * Cumple con la parte de "Autenticación y Seguridad" del proyecto:
 *   - Implementa cifrado de contraseñas con BCrypt.
 *   - Usa JWT para autenticar y autorizar usuarios.
 *   - Restringe acceso según roles definidos.
 */

@Service // Indica que esta clase es un servicio de Spring y se gestionará como un bean
public class AuthenticationService {
    //Repositorio para acceder a los usuarios en la base de datos
    private final IUserRepository userRepository;
    //Codificador de contraseñas (BCrypt en este caso)
    private final PasswordEncoder passwordEncoder;
    //Servicio para generar y validar JWT
    private final JwtService jwtService;
    //Componente de spring security para manejar la autenticación
    private final AuthenticationManager authenticationManager;

    //Constructor que inyecta las dependencias requeridas
    public AuthenticationService(
            IUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Método para registrar un nuevo usuario.
     * - Convierte el rol recibido como texto a un valor del enum Role.
     * - Si el rol es inválido o nulo, se asigna por defecto Role.USER.
     * - Codifica la contraseña usando BCrypt antes de guardarla.
     * - Guarda el usuario en la base de datos.
     * - Genera un token JWT para el nuevo usuario.
     */
//    public AuthenticationResponse register(RegisterRequest request) {
//       Role role;
//       try {
//           // Convierte el string del request a un valor válido del enum Role
//           role = Role.valueOf(request.getRole().toUpperCase());
//       }catch (IllegalArgumentException | NullPointerException e) {
//           // Si no es válido, asigna el rol USER por defecto
//           role = Role.ADMIN;
//       }
//        // Construcción del objeto User sin builder
//        User user = new User();
//        user.setEmail(request.getEmail());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setRole(role);
//        // Guardado del nuevo usuario en la bd
//        userRepository.save(user);
//
//        // Genera un token JWT para el usuario recién creado
//        var jwt = jwtService.generateToken(user);
//
//        //Devuelve el token en la respuesta
//        return AuthenticationResponse.builder()
//                .token(jwt)
//                .build();
//    }

    /**
     * Registra un nuevo administrador en el sistema.
     * <p>
     * Este método está diseñado para ser invocado únicamente por usuarios con rol ADMIN.
     * Realiza las siguientes operaciones:
     * 1. Verifica que el email del nuevo administrador no exista previamente
     * 2. Encripta la contraseña utilizando BCrypt
     * 3. Guarda el nuevo usuario con rol ADMIN
     *
     * @param adminDTO datos del nuevo administrador (email y contraseña)
     * @throws RuntimeException si el email ya está registrado en el sistema
     */
    public void registerAdmin(AdminRegisterDTO adminDTO) {
        // Verificar que el email no esté ya registrado
        if (userRepository.findByEmail(adminDTO.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear y configurar el nuevo usuario administrador
        User newAdmin = new User();
        newAdmin.setEmail(adminDTO.getEmail());
        newAdmin.setPassword(passwordEncoder.encode(adminDTO.getPassword()));
        newAdmin.setRole(Role.ADMIN); // Asignar específicamente el rol ADMIN

        // Guardar el nuevo administrador
        userRepository.save(newAdmin);
    }

    /**
     * Método para iniciar sesión.
     * - Autentica las credenciales usando el AuthenticationManager.
     * - Busca el usuario en la base de datos.
     * - Genera un token JWT para ese usuario.
     */
    public AuthenticationResponse login(AuthenticationRequest request) {
        // Autenticación: verifica que el email y password coincidan
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Busca el usuario por email, lanza excepción si no existe
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        // Genera un token JWT para ese usuario
        var jwtToken = jwtService.generateToken(user);

        // Devuelve el token en la respuesta
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
