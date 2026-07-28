package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.UserDTO;
import com.gym.gym_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión de usuarios administradores del sistema.
 *
 * <p>El controlador recibe peticiones HTTP y delega la lógica de negocio en un servicio.
 * Esta separación mantiene el código más fácil de probar y de mantener.</p>
 *
 * <p>Expone endpoints para consultar usuarios con rol {@code ADMIN}. El acceso está
 * protegido: solo un usuario autenticado con ese rol puede utilizarlo.</p>
 */
// @RestController registra esta clase como componente de Spring y hace que sus métodos
// respondan directamente con datos, normalmente JSON, en lugar de una vista HTML.
@RestController
// @RequestMapping define la ruta común de los endpoints de esta clase. Por eso el método
// getAllAdmins(), que usa @GetMapping sin ruta, queda disponible en GET /api/admins.
@RequestMapping("/api/admins")
// @PreAuthorize comprueba esta expresión antes de ejecutar los métodos. hasRole('ADMIN')
// exige la autoridad ROLE_ADMIN; es importante para proteger la información de usuarios.
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    // Dependencia que Spring inyecta automáticamente. El controlador no crea el servicio
    // con 'new', permitiendo que Spring gestione su ciclo de vida y configuración.
    private final UserService userService;

    /**
     * Constructor con inyección de dependencias.
     *
     * <p>La inyección por constructor deja explícita la dependencia obligatoria y permite
     * que el atributo sea {@code final}. Spring proporciona la instancia de
     * {@link UserService} al crear el controlador.</p>
     *
     * @param userService servicio para la gestión de usuarios
     */
    // @Autowired indica a Spring qué constructor debe usar para resolver e inyectar
    // las dependencias. Con un único constructor moderno puede omitirse, pero aquí
    // se conserva para mostrar claramente el mecanismo.
    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Obtiene la lista de todos los administradores registrados en el sistema.
     *
     * <p>Delega en el servicio, que filtra por rol y convierte cada entidad en un
     * {@link UserDTO}. Un DTO es importante porque evita devolver campos internos o
     * sensibles, como la contraseña.</p>
     *
     * @return respuesta HTTP 200 con la lista de administradores en formato DTO
     */
    // @GetMapping relaciona este método con peticiones HTTP GET. Al no recibir una ruta,
    // reutiliza la de @RequestMapping: GET /api/admins.
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllAdmins() {
        // ok(...) construye una respuesta 200 OK con la lista en el cuerpo HTTP.
        return ResponseEntity.ok(userService.findAllAdmins());
    }
}
