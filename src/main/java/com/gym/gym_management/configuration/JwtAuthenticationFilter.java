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
     *
     * Pasos:
     * 1. Obtiene el header "Authorization".
     * 2. Verifica si existe y si empieza con "Bearer ".
     * 3. Extrae el token JWT y obtiene el email del usuario.
     * 4. Comprueba si no hay autenticación previa en el contexto.
     * 5. Carga los datos del usuario desde la base de datos.
     * 6. Valida que el token sea correcto y no esté expirado.
     * 7. Crea un objeto de autenticación y lo guarda en el contexto de seguridad.
     * 8. Continúa la cadena de filtros.
     */
    @Override
    protected void doFilterInternal(
             @NonNull HttpServletRequest request,
             @NonNull HttpServletResponse response,
             @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Obtiene la cabecera de autorización
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Si no hay cabecera o no empieza con "Bearer ", pasa al siguiente filtro sin hacer nada
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 3. Extrae el token quitando el prefijo "Bearer "
        jwt = authHeader.substring(7);

        // 4. Obtiene el email del usuario desde el token
        userEmail = jwtService.extractUsername(jwt);

        // 5. Si hay un usuario y no existe autenticación previa en el contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 6. Carga los detalles del usuario desde la base de datos
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 7. Valida el token comparándolo con los datos del usuario
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Crea un objeto de autenticación con los permisos del usuario
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                // Añade detalles adicionales sobre la solicitud
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // 8. Guarda la autenticación en el contexto de seguridad de Spring
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
            // 9. Continúa con el siguiente filtro de la cadena
            filterChain.doFilter(request, response);
        }

    }
}
