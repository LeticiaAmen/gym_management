package com.gym.gym_management.security;

import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IUserRepository;
import com.gym.gym_management.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para verificar el rate limiting en endpoints sensibles:
 * - /auth/login (intentos de autenticación)
 * - /api/password/request-reset (solicitudes de recuperación de contraseña)
 * <p>
 * Se configuran límites bajos (maxAttempts=3) para acelerar las pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Reducimos límites para pruebas rápidas
        "security.ratelimit.login.maxAttempts=3",
        "security.ratelimit.login.blockMinutes=5",
        "security.ratelimit.passwordReset.maxAttempts=3",
        "security.ratelimit.passwordReset.blockMinutes=5",
        // Configuración mínima necesaria
        "spring.datasource.url=jdbc:h2:mem:ratelimittest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "JWT_SECRET=VGhpcy1yYXRlbGltaXQtdGVzdC1zZWNyZXQtYmFzZTY0LXN0cmluZw==",
        "app.email.enabled=false"
})
public class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender javaMailSender; // evitar envío real

    @Autowired
    private RateLimitService rateLimitService;

    private static final String VALID_EMAIL = "admin@gym.com";
    private static final String VALID_PASSWORD = "AdminPass1";

    @BeforeEach
    void setUp() {
        rateLimitService.clearAll();
        userRepository.deleteAll();
        User u = new User();
        u.setEmail(VALID_EMAIL);
        u.setPassword(passwordEncoder.encode(VALID_PASSWORD));
        u.setRole(Role.ADMIN);
        userRepository.save(u);
    }

    @Test
    @DisplayName("Bloquea login después de exceder intentos fallidos")
    void deberiaBloquearLoginTrasExcederIntentos() throws Exception {
        // 1er intento fallido
        failLogin();
        // 2do
        failLogin();
        // 3er (al alcanzar maxAttempts=3 queda bloqueado en registro)
        failLogin();
        // 4to intento: debe devolver 429 (bloqueado por rate limit)
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\",\"password\":\"Wrong1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Login exitoso reinicia contador de intentos")
    void loginExitosoReiniciaIntentos() throws Exception {
        // 2 fallos iniciales
        failLogin();
        failLogin();
        // éxito
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\",\"password\":\""+VALID_PASSWORD+"\"}"))
                .andExpect(status().isOk());
        // Nueva serie de fallos debe comenzar limpia (no bloquea aún)
        failLogin(); // 1
        failLogin(); // 2
        // aún no bloqueado
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\",\"password\":\"Wrong1\"}"))
                .andExpect(status().isUnauthorized()); // 3er fallo; bloquea a partir del próximo
        // Próximo intento debe estar bloqueado
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\",\"password\":\"Wrong1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Bloquea solicitudes de password reset tras exceder límite")
    void deberiaBloquearPasswordResetTrasLimite() throws Exception {
        requestReset(); // 1
        requestReset(); // 2
        requestReset(); // 3 -> alcanza límite
        // 4 -> bloqueado
        mockMvc.perform(post("/api/password/request-reset")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\"}"))
                .andExpect(status().isTooManyRequests());
    }

    private void failLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\",\"password\":\"Wrong1\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void requestReset() throws Exception {
        mockMvc.perform(post("/api/password/request-reset")
                        .contentType("application/json")
                        .content("{\"email\":\""+VALID_EMAIL+"\"}"))
                .andExpect(status().isOk());
    }
}
