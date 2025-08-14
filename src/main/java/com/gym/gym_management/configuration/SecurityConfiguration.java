package com.gym.gym_management.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * Configuración principal de seguridad de Spring Security para la aplicación.
 *
 * Funciones clave:
 * - Define las reglas de autorización (qué endpoints son públicos y cuáles requieren autenticación).
 * - Integra el filtro de validación de JWT para todas las solicitudes protegidas.
 * - Configura la política de sesiones como "STATELESS" para trabajar sin sesiones (solo con JWT).
 *
 * Relación con los requerimientos:
 * Cumple con la parte de "Autenticación y Seguridad":
 *   - Restringe acceso por roles.
 *   - Protege todos los endpoints excepto el login.
 *   - Obliga al uso de JWT para acceder a recursos protegidos.
 */

@Configuration //Marca la clase como configuración de spring
@EnableWebSecurity // Activa la configuración personalizada de seguridad web
@EnableMethodSecurity //Permite usar anotaciones como @PreAuthorize en métodos
public class SecurityConfiguration {

    // Filtro que intercepta cada request y valida el JWT
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    //Proveedor de autenticación configurado en ApplicationConfig
    private final AuthenticationProvider authenticationProvider;

    // Constructor manual que inyecta el filtro JWT y el proveedor de autenticación.
    public SecurityConfiguration(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
    }


    /**
     * -------SecurityFilterChain---------
     * Define la cadena de filtros y configuraciones de seguridad de la aplicación.
     *
     * Configuración:
     * 1. Deshabilita CSRF (no necesario con JWT y APIs REST).
     * 2. Reglas de autorización:
     *    - Permitir acceso público al endpoint POST /auth/login.
     *    - Restringir /auth/register a usuarios con rol USER.
     *    - Proteger todos los demás endpoints (requieren autenticación).
     * 3. Configura sesiones como STATELESS (sin estado), para que no se usen cookies de sesión.
     * 4. Registra el AuthenticationProvider configurado en ApplicationConfig.
     * 5. Añade el filtro JWT antes del filtro por defecto de autenticación de usuario/contraseña.
     *
     * @param http objeto de configuración de seguridad HTTP.
     * @return la cadena de filtros de seguridad configurada.
     * @throws Exception si ocurre un error al construir la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Deshabilita CSRF (no necesario en APIs REST con JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Reglas de autorización
                .authorizeHttpRequests(
                        auth -> auth
                                // Permitir acceso público solo para login; registrar requiere rol USER
                                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                                .requestMatchers(HttpMethod.POST, "/auth/register").hasRole("USER")
                                .anyRequest().authenticated() // Todo lo demás requiere autenticación

                )
                // 3. Configura sesiones sin estado
                .sessionManagement(
                        session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. Define el proveedor de autenticación
                .authenticationProvider(authenticationProvider)

                // 5. Añade el filtro JWT antes de UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
