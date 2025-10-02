/**
 * Tests de integración para el flujo completo de recuperación de contraseña.
 * <p>
 * Estos tests validan la interacción real entre capas (controller + service + repository)
 * ejecutando sobre un contexto Spring Boot con H2 en memoria. Se cubren:
 * <ul>
 *     <li>Solicitud de recuperación y generación de token.</li>
 *     <li>Validación de token válido.</li>
 *     <li>Reseteo exitoso de contraseña y uso único del token.</li>
 *     <li>Intento de reseteo con token inválido.</li>
 *     <li>Token expirado.</li>
 *     <li>Solicitud con email inexistente (respuesta genérica y sin fuga de información).</li>
 * </ul>
 * Se utiliza reflexión para acceder al mapa interno de tokens en {@link com.gym.gym_management.service.PasswordResetService}
 * ya que en esta versión se almacenan solo en memoria (no persistidos en BD). Esto permite:
 * <ol>
 *     <li>Extraer el token generado tras la solicitud.</li>
 *     <li>Limpiar el estado entre tests.</li>
 *     <li>Inyectar un token expirado artificialmente.</li>
 * </ol>
 */
package com.gym.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IUserRepository;
import com.gym.gym_management.service.PasswordResetService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pwdresettst;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=never",
        "JWT_SECRET=VGhpcy1kZWZhdWx0LXRlc3Qtc2VjcmV0LWxvbmcgc3RyaW5nIQ==",
        "app.email.enabled=false"
})
public class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender javaMailSender; // Mock para evitar necesidad de infraestructura SMTP

    private static final String EXISTING_EMAIL = "admin@gym.com"; // se crea manualmente en cada test
    private static final String ORIGINAL_PASSWORD = "OldPass1";   // en claro para verificación (se encripta antes de guardar)

    /** Limpia tokens antes de cada test para asegurar aislamiento. */
    @BeforeEach
    void clearTokensAndSeedUser() throws Exception {
        clearInternalTokens();
        userRepository.deleteAll();
        User user = new User();
        user.setEmail(EXISTING_EMAIL);
        user.setPassword(passwordEncoder.encode(ORIGINAL_PASSWORD));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    /**
     * Verifica el flujo completo feliz: solicitud -> validación -> confirmación -> token inválido tras uso.
     * @throws Exception si ocurre un error en el flujo MockMvc
     */
    @Test
    public void deberiaCompletarFlujoRecuperacionExitosoYTokenUnUso() throws Exception {
        int tokensAntes = contarTokens();

        // 1. Solicitar recuperación
        mockMvc.perform(post("/api/password/request-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EXISTING_EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Si el correo existe")));

        // 2. Obtener token generado
        String token = extraerUnicoTokenNuevo(tokensAntes);
        assertThat(token).isNotBlank();

        // 3. Validar token
        mockMvc.perform(get("/api/password/validate-token/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 4. Confirmar reseteo con nueva contraseña válida (cumple regex)
        String newPassword = "NuevoPass1"; // mayúscula, minúscula, número, >=8
        mockMvc.perform(post("/api/password/confirm-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"token\":\"" + token + "\"," +
                                "\"newPassword\":\"" + newPassword + "\"," +
                                "\"confirmPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Contraseña actualizada correctamente")));

        // 5. Verificar que la contraseña realmente cambió en BD
        User updated = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, updated.getPassword())).isFalse();

        // 6. Reintentar uso del mismo token -> debe fallar (token ya invalidado)
        mockMvc.perform(post("/api/password/confirm-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"token\":\"" + token + "\"," +
                                "\"newPassword\":\"OtraPass1\"," +
                                "\"confirmPassword\":\"OtraPass1\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que un token inexistente no permite resetear la contraseña.
     * @throws Exception si ocurre un error en MockMvc
     */
    @Test
    public void noDeberiaPermitirResetConTokenInvalido() throws Exception {
        mockMvc.perform(post("/api/password/confirm-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"token\":\"tokenInexistente\"," +
                                "\"newPassword\":\"NuevaPass1\"," +
                                "\"confirmPassword\":\"NuevaPass1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("token es inválido")));
    }

    /**
     * Simula un token expirado insertándolo manualmente y valida que el endpoint lo marque como inválido.
     * @throws Exception si ocurre un error en MockMvc o reflexión
     */
    @Test
    public void deberiaMarcarTokenExpiradoComoInvalido() throws Exception {
        String expiredToken = "tokenExpirado123";
        inyectarToken(expiredToken, EXISTING_EMAIL, LocalDateTime.now().minusMinutes(10));

        mockMvc.perform(get("/api/password/validate-token/" + expiredToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    /**
     * Verifica que solicitar recuperación con email inexistente devuelve 200 (para no filtrar existencia)
     * pero no incrementa la cantidad de tokens almacenados.
     * @throws Exception si ocurre un error en MockMvc
     */
    @Test
    public void solicitudConEmailInexistenteNoDebeCrearToken() throws Exception {
        int tokensAntes = contarTokens();

        mockMvc.perform(post("/api/password/request-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noexiste@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Si el correo existe")));

        assertThat(contarTokens()).isEqualTo(tokensAntes);
    }

    // ------------------ Métodos utilitarios privados (sin JavaDoc por ser detalle interno) ------------------

    private void clearInternalTokens() throws Exception {
        Field f = PasswordResetService.class.getDeclaredField("resetTokens");
        f.setAccessible(true);
        Map<?,?> map = (Map<?,?>) f.get(passwordResetService);
        map.clear();
    }

    @SuppressWarnings("unchecked")
    private int contarTokens() throws Exception {
        Field f = PasswordResetService.class.getDeclaredField("resetTokens");
        f.setAccessible(true);
        return ((Map<String, ?>) f.get(passwordResetService)).size();
    }

    @SuppressWarnings("unchecked")
    private String extraerUnicoTokenNuevo(int tokensAntes) throws Exception {
        Field f = PasswordResetService.class.getDeclaredField("resetTokens");
        f.setAccessible(true);
        Map<String, ?> tokens = (Map<String, ?>) f.get(passwordResetService);
        assertThat(tokens.size()).isGreaterThan(tokensAntes);
        Set<String> keys = tokens.keySet();
        // Retorna el primero (solo se genera uno por test)
        return keys.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private void inyectarToken(String token, String email, LocalDateTime expiry) throws Exception {
        // Acceder al mapa
        Field mapField = PasswordResetService.class.getDeclaredField("resetTokens");
        mapField.setAccessible(true);
        Map<String, Object> map = (Map<String, Object>) mapField.get(passwordResetService);

        // Crear instancia de la clase interna PasswordResetToken
        Class<?> tokenClass = Class.forName("com.gym.gym_management.service.PasswordResetService$PasswordResetToken");
        Constructor<?> ctor = tokenClass.getDeclaredConstructor(String.class, LocalDateTime.class);
        ctor.setAccessible(true);
        Object tokenObj = ctor.newInstance(email, expiry);

        map.put(token, tokenObj);
    }
}
