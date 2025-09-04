package com.gym.gym_management.configuration;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.repository.IClientRepository;
import com.gym.gym_management.repository.IUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Clase de configuración principal para la seguridad y autenticación en la aplicación.
 *
 * Funciones clave:
 * - Define cómo se cargan los detalles de un usuario desde la base de datos.
 * - Configura el proveedor de autenticación usando DAO y BCrypt.
 * - Expone beans para `AuthenticationManager` y `PasswordEncoder`.
 *
 * Relación con los requerimientos:
 * Cumple con el apartado de "Autenticación y Seguridad" del sistema:
 *   - Carga de usuarios desde la base de datos.
 *   - Cifrado de contraseñas con BCrypt.
 *   - Restricción de acceso por roles mediante Spring Security.
 *
 * Esta configuración es utilizada por Spring Security para gestionar
 * el proceso de autenticación y autorización.
 */

@Configuration //Indica que esta clase define configuraciones de spring
public class ApplicationConfig {

    //repositorios que permiten acceder a los usuarios y clientes en la base de datos
    private final IUserRepository userRepository;
    private final IClientRepository clientRepository;

    /**
     * Constructor que recibe el repositorio de usuarios por inyección de dependencias.
     * @param userRepository interfaz para consultar y manipular usuarios en la BD.
     * @param clientRepository interfaz para consultar la información de clientes
     */
    public ApplicationConfig(IUserRepository userRepository, IClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Bean que implementa la interfaz UserDetailsService.
     *
     * Spring Security invoca este servicio cuando un usuario intenta iniciar sesión.
     * Su función es obtener desde la base de datos los datos del usuario (email, contraseña y roles).
     * Estos datos luego se usan para validar las credenciales y aplicar las restricciones de seguridad.
     *
     * Flujo:
     * - Se busca el usuario en la base de datos a partir de su email (username).
     * - Si no existe, se lanza una UsernameNotFoundException.
     * - Si el usuario es un CLIENTE, se valida además si su cuenta está activa.
     *   → Si el cliente está inactivo, se lanza DisabledException para impedir su acceso.
     * - Si pasa las validaciones, se retorna el objeto `user` que Spring Security utilizará.
     *
     * @return implementación de UserDetailsService basada en una expresión lambda.
     * @throws UsernameNotFoundException si no existe el usuario en la base de datos.
     * @throws DisabledException si el cliente está marcado como inactivo.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // Se retorna una lambda que implementa el método loadUserByUsername de UserDetailsService.
        return username -> {
            // Busca el usuario en la BD
            // Si no lo encuentra, lanza una excepción específica que Spring Security entiende.
            var user = userRepository.findByEmail(username)
                    .orElseThrow(()-> new UsernameNotFoundException("Usuario no encontrado"));

            // Si el usuario pertenece al rol CLIENTE, se hace una validación extra:
            if (user.getRole() == Role.CLIENT){
                // Se busca el cliente asociado a ese usuario (en otra tabla/entidad).
                Client client = clientRepository.findByUserEmail(user.getEmail());

                // Si existe y está inactivo, no se permite iniciar sesión.
                if (client != null && !client.isActive()){
                    throw new DisabledException("Cuenta de usuario desactivada");
                }
            }
            // Si no hay problemas, se retorna el usuario.
            // Este objeto debe implementar UserDetails para que Spring Security pueda usarlo.
            return user;
        };
    }

    /**
     * Bean que define el proveedor de autenticación.
     * Usa `DaoAuthenticationProvider` para obtener los usuarios desde la base de datos
     * a través de `UserDetailsService` y compara las contraseñas usando BCrypt.
     *
     * @return proveedor de autenticación configurado.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService()); // Carga usuario
        authenticationProvider.setPasswordEncoder(passwordEncoder()); // Verifica contraseña encriptada
        return authenticationProvider;
    }

    /**
     * Bean que expone el AuthenticationManager de Spring Security.
     *
     * Este componente coordina el proceso de autenticación
     * usando el `AuthenticationProvider` configurado.
     *
     * @param configuration configuración de autenticación de Spring.
     * @return AuthenticationManager listo para usarse en servicios/controladores.
     * @throws Exception si no puede obtener el AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
    throws Exception {
        // Proporciona el AuthenticationManager usado por Spring Security
        return configuration.getAuthenticationManager();
    }

    /**
     * Bean que define el codificador de contraseñas.
     * Usa BCrypt, recomendado por su resistencia frente a ataques de fuerza bruta.
     * @return instancia de BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
