package com.gym.gym_management.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio encargado de generar, firmar, extraer y validar tokens JWT.
 *
 * Funciones principales:
 * - Crear tokens JWT firmados con una clave secreta.
 * - Extraer información (claims) de un token.
 * - Validar si un token es válido o ha expirado.
 *
 * Relación con los requerimientos:
 * Cumple con el apartado "Autenticación y Seguridad" implementando
 * autenticación con JWT para proteger los endpoints de la aplicación.
 *
 * Notas de configuración:
 * - La clave secreta se obtiene del archivo de configuración (application.properties o .yml)
 *   mediante la propiedad `jwt.secret`.
 * - El algoritmo de firma utilizado es HMAC-SHA256 (HS256).
 */
@Service //indica que es un servicio gestionado por spring
public class JwtService {
    //Clave secreta utilizada para firmar y validar los JWT
    //Spring la inyecta automáticamente desde la configuración
    @Value("${jwt.secret}")
    private String secretKey;      // Spring la rellena al arrancar

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     * @param token token JWT.
     * @return nombre de usuario incluido en el token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Genera un token JWT usando un objeto UserDetails.
     * @param userDetails detalles del usuario (email, roles, etc.).
     * @return token JWT firmado.
     */
    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }


    /**
     * Genera un token JWT con claims personalizados.
     *
     * Pasos:
     * 1. Añade los claims (datos extra opcionales).
     * 2. Define el subject (nombre de usuario).
     * 3. Define fecha de emisión y expiración (24 horas desde la emisión).
     * 4. Firma el token con la clave secreta y el algoritmo HS256.
     *
     * @param extractClaims claims adicionales a incluir en el token.
     * @param userDetails detalles del usuario autenticado.
     * @return token JWT firmado.
     */
    public String generateToken(
            Map<String, Object> extractClaims,
            UserDetails userDetails
    ){
        return Jwts
                .builder()
                .setClaims(extractClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Genera un token JWT directamente desde un objeto Authentication.
     * Útil cuando ya se tiene el usuario autenticado por Spring Security.
     * @param authentication objeto de autenticación de Spring.
     * @return token JWT firmado.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Valida si un token es válido para un usuario específico.
     * @param token token JWT.
     * @param userDetails detalles del usuario.
     * @return true si el token pertenece al usuario y no ha expirado.
     */
    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username =  extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

     //Comprueba si un token ha expirado
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    //Obtiene la fecha de expiración de un token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token.
     * @param token token JWT.
     * @param claimsTFunction función que indica qué claim obtener.
     * @return valor del claim solicitado.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsTFunction){
        final Claims claims = extractAllClaims(token);
        return claimsTFunction.apply(claims);
    }

    /**
     * Extrae todos los claims del token.
     * @param token token JWT.
     * @return objeto Claims con toda la información del token.
     */
    public Claims extractAllClaims(String token){
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey()) // Define la clave de verificación
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Obtiene la clave secreta usada para firmar/verificar tokens.
     * La clave se decodifica desde Base64 y se transforma en un objeto Key.
     * @return clave de firma HMAC-SHA256.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
