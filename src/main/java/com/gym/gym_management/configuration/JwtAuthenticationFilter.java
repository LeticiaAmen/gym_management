package com.gym.gym_management.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Filtro de seguridad que intercepta cada solicitud HTTP para validar un token JWT.
 *
 * Funciones principales:
 * - Extrae el token JWT de la cabecera "Authorization" (prefijo "Bearer ").
 * - Valida el token y obtiene el usuario asociado.
 * - Si el token es válido, crea la autenticación y la guarda en el contexto de seguridad de Spring.
 *
 * Relación con los requerimientos:
 * Cumple con el apartado "Autenticación y Seguridad" al garantizar que solo
 * usuarios con un token válido puedan acceder a recursos protegidos.
 *
 * Extiende OncePerRequestFilter:
 * Esto asegura que el filtro se ejecute una sola vez por cada solicitud HTTP.
 */

@Component //Indica que es un componente gestionado por Spring
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Servicio encargado de generar, extraer y validar tokens JWT.
    private final JwtService jwtService;

    // Servicio de Spring Security para cargar los detalles del usuario desde la base de datos.
    private final UserDetailsService userDetailsService;

    /**
     * Constructor que inyecta JwtService y UserDetailsService.
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Método que se ejecuta en cada solicitud HTTP antes de llegar al controlador.
     */
    @Override
    protected void doFilterInternal(
             @NonNull HttpServletRequest request,
             @NonNull HttpServletResponse response,
             @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Sin token: continuar la cadena (rutas públicas o fallará por falta de auth según SecurityConfig)
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        // Siempre continuar la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
