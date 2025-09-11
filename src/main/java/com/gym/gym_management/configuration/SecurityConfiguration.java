package com.gym.gym_management.configuration;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // IMPORTANTE: requerido para deshabilitar CSRF/formLogin/httpBasic
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;



@Configuration //Marca la clase como configuración de spring
@EnableWebSecurity // Activa la configuración personalizada de seguridad web
@EnableMethodSecurity //Permite usar anotaciones como @PreAuthorize en métodos
public class SecurityConfiguration {

    // Filtro que intercepta cada request y valida el JWT
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    //Proveedor de autenticación (valida usuarios contra la BD con PasswordEncoder)
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
     * Define la cadena de filtros principal de seguridad.
     * Aquí configuramos todas las reglas de autorización y autenticación.
     *
     * Notas clave (MVP admin-only):
     * - API completamente stateless con JWT: no hay sesión de servidor.
     * - Todos los endpoints de gestión requieren ROLE_ADMIN.
     * - Vistas/estáticos pueden ser públicos; la protección efectiva está en la API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitamos CSRF porque usamos JWT (API sin estado, no formularios HTML clásicos).
                .csrf(AbstractHttpConfigurer::disable)

                // Política de sesión: sin estado. Cada request debe traer su JWT.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reglas de autorización
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos comunes (favicon, css, js, img)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/index.html", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/assets/**").permitAll()

                        // HTML del panel admin puede servir público; la API está protegida por JWT+RBAC.
                        .requestMatchers("/admin/**").permitAll()

                        // Endpoint público para obtener JWT
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()

                        // Endpoints de gestión ADMIN-only (prefijo actual /api/)
                        .requestMatchers("/api/clients/**").hasRole("ADMIN")
                        .requestMatchers("/api/payments/**").hasRole("ADMIN")
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        // Compatibilidad si existieran rutas sin prefijo /api
                        .requestMatchers("/clients/**").hasRole("ADMIN")
                        .requestMatchers("/payments/**").hasRole("ADMIN")
                        .requestMatchers("/reports/**").hasRole("ADMIN")

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated()
                )
                // Deshabilitamos mecanismos de login tradicionales de Spring. Usamos solo JWT.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Si falta autenticación o el token es inválido devolvemos 401 (sin redirecciones)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))

                // Registramos el AuthenticationProvider (UserDetailsService + PasswordEncoder)
                .authenticationProvider(authenticationProvider)

                // Agregamos el filtro JWT ANTES del filtro estándar de Spring
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Finalmente devolvemos la cadena de seguridad configurada
        return http.build();
    }

}
