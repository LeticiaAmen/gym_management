package com.gym.gym_management.configuration;


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
 * Se definen dos cadenas de filtros:
 * - Una para los endpoints REST que siguen utilizando autenticación JWT
 * - Otra para las páginas web que usan sesiones y formulario de login
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
     * Cadena de filtros para los endpoints REST protegidos con JWT
     * CSRF se deshabilita y las sesiones se mantienen sin estado
     */
    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http

                .securityMatcher("/auth/**", "/clients/**", "/payments/**", "/users/**")
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Cadena de filtros para las páginas web
     * Se habilita login basado en formulario y se crean sesiones solo cuando es necesario
     */
    @Bean
    public SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/index.html", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/login").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.loginPage("/login").permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider);
        // CSRF se mantiene habilitado por defecto para las peticiones de formulario
        return http.build();
    }
}
