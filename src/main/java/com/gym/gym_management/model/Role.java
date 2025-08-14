package com.gym.gym_management.model;
/**
 * Enum que representa los roles de usuario en la aplicación.
 *
 * Valores:
 * - USER   → Administrador del sistema (puede gestionar clientes y pagos).
 * - CLIENT → Cliente del gimnasio (puede consultar su historial y estado de pagos).
 *
 * Relación con los requerimientos:
 * - "Usuarios y Roles": define los permisos en la aplicación.
 *   - Solo los administradores (USER) pueden registrar nuevos clientes y gestionar pagos.
 *   - Los clientes (CLIENT) solo pueden acceder a su información y consultar pagos.
 * - Usado en Spring Security para restringir el acceso a ciertos endpoints
 *   mediante anotaciones como @PreAuthorize("hasRole('USER')").
 */

public enum Role {
    USER,
    CLIENT
}
