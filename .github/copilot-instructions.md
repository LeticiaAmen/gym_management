Usa nombres de clases en PascalCase y nombres de variables en camelCase.
Los métodos deben ser cortos, hacer una sola cosa y tener nombres descriptivos.
No generes código duplicado, extrae lógica repetida a métodos privados o utilitarios. 

Usa anotaciones estándar de Spring. 
Implementa DTOs para exponer datos en la API y evita devolver entidades directamente. 
Maneja la seguridad con Spring Security y JWT, restringiendo endpoints según roles. 

Usa JpaRepository en lugar de consultas manuales cuando sea posible.
Define relaciones con @OneToMany, @ManyToOne, etc., y usa @JsonIgnore para evitar ciclos en JSON.
Implementa bajas lógicas (ej. isActive en Client) en lugar de borrados físicos.

Valida datos de entrada con @Valid y @NotNull, @Email, etc.
Maneja excepciones con un @ControllerAdvice global que devuelva respuestas claras (HTTP 400/404/500).
Evita capturar excepciones genéricas; captura específicas y lanza custom exceptions si hace falta.

Almacena contraseñas con BCrypt (PasswordEncoder).
Nunca expongas datos sensibles en logs ni respuestas JSON.
Configura la API como stateless (sin sesiones de servidor, solo JWT).

Aplica principios SOLID.
Evita hardcodear strings o valores mágicos (usa constantes).

Por favor, genera comentarios JavaDoc en las clases y métodos del proyecto siguiendo estas reglas:

1. **Clases e interfaces públicas**: agregar una descripción clara de su propósito dentro del sistema.
2. **Métodos públicos**: documentar detalladamente qué hacen, explicando su lógica y relación con el dominio del gimnasio (clientes, pagos, reportes, etc.).
    - Incluir etiquetas `@param`, `@return` y `@throws` cuando corresponda.
3. **Constructores**: documentar los parámetros y su propósito si no son triviales.
4. **Constantes públicas**: explicar su significado en el negocio.
5. **No generar JavaDoc** en getters, setters, métodos privados triviales o sobrecargas obvias, salvo que tengan lógica no evidente.
6. Usar un lenguaje claro y explicativo, como si estuvieras ayudando a un programador junior a entender el código.
7. Si la lógica del método es compleja, generar una descripción paso a paso de lo que hace.

Ejemplo esperado:

```java
/**
 * Registra un nuevo pago en el sistema.
 * <p>
 * Este método recibe un DTO con los datos del pago, lo convierte en entidad y lo guarda en la base de datos.
 * Además, valida que el cliente exista y marca el pago como vigente.
 *
 * @param paymentDTO datos del pago (cliente, monto, método, fecha, etc.)
 * @return el pago registrado en formato DTO
 * @throws EntityNotFoundException si el cliente no existe
 */