package com.gym.gym_management.authentication;

import com.gym.gym_management.configuration.JwtService;
import com.gym.gym_management.repository.IUserRepository;
import com.gym.gym_management.model.User;
import com.gym.gym_management.model.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public AuthenticationResponse register(RegisterRequest request) {
       Role role;
       try {
           // Convierte el string del request a un valor válido del enum Role
           role = Role.valueOf(request.getRole().toUpperCase());
       }catch (IllegalArgumentException | NullPointerException e) {
           // Si no es válido, asigna el rol USER por defecto
           role = Role.USER;
       }
        // Construcción del objeto User con el patrón Builder
        var user = User.builder()
                .email(request.getEmail()) // Email como nombre de usuario
                .password(passwordEncoder.encode(request.getPassword())) // Contraseña encriptada
                .role(role) //Rol definido
                .build();
       // Guardado del nuevo usuario en la bd
        userRepository.save(user);

        // Genera un token JWT para el usuario recién creado
        var jwt = jwtService.generateToken(user);

        //Devuelve el token en la respuesta
        return AuthenticationResponse.builder()
                .token(jwt)
                .build();
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

        // Genera un nuevo token JWT para este usuario
        var jwt = jwtService.generateToken(user);
        // Devuelve el token en la respuesta
        return AuthenticationResponse.builder()
                .token(jwt)
                .build();

    }




}
