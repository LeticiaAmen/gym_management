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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * Configuración de seguridad de Spring Security para la aplicación.
 *
 * Objetivo:
 *  - Proteger los endpoints REST de la aplicación usando autenticación con JWT.
 *  - Permitir el acceso público a los recursos estáticos (HTML, CSS, JS, imágenes).
 *  - Evitar redirecciones al login por defecto de Spring Security (devolvemos 401 en su lugar).
 *  - Definir reglas claras de autorización para separar qué rutas son públicas
 *    y cuáles requieren autenticación.
 */

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
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitamos CSRF porque usamos JWT (API sin estado, no formularios HTML clásicos).
                .csrf(AbstractHttpConfigurer::disable)

                // Definimos política de sesión: sin estado
                // Cada request debe traer su JWT, no se guarda nada en la sesión del servidor
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // definimos que rutas son públicas y cuales requieren autenticación
                .authorizeHttpRequests(auth -> auth
                        //Recursos estáticos comunes (favicosn, css. js, img)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/index.html", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/assets/**").permitAll()

                        // Páginas HTML del frontend (panel admin y cliente) también son públicas
                        // la seguridad real está en la api, no es estos html
                        .requestMatchers("/admin/**", "/client/**").permitAll()

                        //endpoint de login post público
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()

                        // endpoints de la API que requieren autenticación con JWT
                        .requestMatchers("/auth/**", "/clients/**", "/payments/**", "/users/**").authenticated()

                        //cualquier otra ruta no contemplada requiere autenticación
                        .anyRequest().authenticated()
                )
                //Deshabilitamos los mecanismos de login tradicionales de Spring (formLogin y httpBasic).
                // Solo usamos JWT.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Si falta autenticación o el token es inválido  devolvemos 401 en lugar de redirigir a /login.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))

                //Registramos el AuthenticationProvider (usa UserDetailsService + PasswordEncoder)
                .authenticationProvider(authenticationProvider)

                //Agregamos el filtro JWT ANTES del filtro estándar de Spring (UsernamePasswordAuthenticationFilter).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        //Finalmente devolvemos la cadena de seguridad configurada
        return http.build();
    }

}
