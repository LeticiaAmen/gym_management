package com.gym.gym_management.service;

import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para la gestión de recuperación y reseteo de contraseñas.
 * <p>
 * Este servicio proporciona métodos para iniciar el proceso de recuperación de contraseña,
 * validar tokens de recuperación y establecer nuevas contraseñas.
 *
 * @author GymManagement
 * @version 1.0
 */
@Service
public class PasswordResetService {

    private final IUserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Almacena tokens de recuperación con timestamp de expiración (en una implementación real debería estar en base de datos)
    private final Map<String, PasswordResetToken> resetTokens = new HashMap<>();

    private static final long TOKEN_VALIDITY_MINUTES = 30; // 30 minutos de validez del token

    @Autowired
    public PasswordResetService(IUserRepository userRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inicia el proceso de recuperación de contraseña para un usuario.
     * <p>
     * Genera un token seguro, lo asocia con el usuario y envía un correo electrónico
     * con el enlace de recuperación.
     *
     * @param email email del usuario que solicita recuperar su contraseña
     * @return true si se inició el proceso correctamente, false si el email no existe
     */
    public boolean initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false; // Usuario no encontrado, pero no revelar esto por seguridad
        }

        User user = userOpt.get();

        // Generar token seguro
        String token = generateSecureToken();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES);

        // Almacenar token con datos del usuario
        resetTokens.put(token, new PasswordResetToken(user.getEmail(), expiryTime));

        // Enviar email con enlace de recuperación
        String resetUrl = "/reset-password.html?token=" + token;
        String emailContent = createResetEmailContent(user.getEmail(), resetUrl);

        emailService.sendEmail(
            user.getEmail(),
            "Recuperación de contraseña - GYM Management",
            emailContent,
            true
        );

        return true;
    }

    /**
     * Verifica si un token de recuperación es válido.
     *
     * @param token token a verificar
     * @return el email del usuario si el token es válido, o null si no es válido o ha expirado
     */
    public String validateToken(String token) {
        PasswordResetToken resetToken = resetTokens.get(token);
        if (resetToken == null || resetToken.isExpired()) {
            return null;
        }

        return resetToken.email;
    }

    /**
     * Establece una nueva contraseña para un usuario usando un token de recuperación.
     *
     * @param token token de recuperación
     * @param newPassword nueva contraseña
     * @return true si se cambió la contraseña correctamente, false en caso contrario
     */
    public boolean resetPassword(String token, String newPassword) {
        String email = validateToken(token);
        if (email == null) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidar el token después de usarlo
        resetTokens.remove(token);

        return true;
    }

    /**
     * Genera un token seguro usando SecureRandom y codificación Base64.
     *
     * @return token seguro para recuperación de contraseña
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Crea el contenido HTML del correo de recuperación de contraseña.
     *
     * @param email email del usuario
     * @param resetUrl URL de recuperación con token
     * @return contenido HTML del correo
     */
    private String createResetEmailContent(String email, String resetUrl) {
        String baseUrl = "http://localhost:8080"; // En producción, usar la URL real
        String fullResetUrl = baseUrl + resetUrl;

        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "    <style>" +
               "        body { font-family: Arial, sans-serif; line-height: 1.6; }" +
               "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
               "        .header { background-color: #1a1d29; color: white; padding: 15px; text-align: center; }" +
               "        .content { padding: 20px; }" +
               "        .button { display: inline-block; background-color: #ff6b35; color: white; " +
               "                 padding: 12px 24px; text-decoration: none; border-radius: 4px; }" +
               "        .footer { font-size: 12px; color: #666; padding-top: 20px; }" +
               "    </style>" +
               "</head>" +
               "<body>" +
               "    <div class='container'>" +
               "        <div class='header'>" +
               "            <h2>Recuperación de Contraseña</h2>" +
               "        </div>" +
               "        <div class='content'>" +
               "            <p>Hola,</p>" +
               "            <p>Has solicitado restablecer tu contraseña para <strong>" + email + "</strong>.</p>" +
               "            <p>Haz clic en el siguiente botón para crear una nueva contraseña:</p>" +
               "            <p style='text-align: center; padding: 20px;'>" +
               "                <a href='" + fullResetUrl + "' class='button'>Restablecer Contraseña</a>" +
               "            </p>" +
               "            <p>Si no solicitaste este cambio, puedes ignorar este correo. " +
               "               El enlace expirará en 30 minutos por seguridad.</p>" +
               "            <p>Si tienes problemas con el botón, copia y pega este enlace en tu navegador:</p>" +
               "            <p>" + fullResetUrl + "</p>" +
               "        </div>" +
               "        <div class='footer'>" +
               "            <p>Este es un correo automático, por favor no respondas a este mensaje.</p>" +
               "            <p>&copy; " + LocalDateTime.now().getYear() + " GYM Management. Todos los derechos reservados.</p>" +
               "        </div>" +
               "    </div>" +
               "</body>" +
               "</html>";
    }

    /**
     * Clase interna para almacenar información de tokens de recuperación de contraseña.
     */
    private static class PasswordResetToken {
        private final String email;
        private final LocalDateTime expiryTime;

        public PasswordResetToken(String email, LocalDateTime expiryTime) {
            this.email = email;
            this.expiryTime = expiryTime;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}
