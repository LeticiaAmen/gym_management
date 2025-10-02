# Visión Técnica (Gym Management)

Este documento describe en profundidad la arquitectura, flujo de datos, decisiones técnicas, convenciones y puntos de extensión del proyecto. El objetivo es que cualquier desarrollador pueda entender rápidamente cómo está construido el sistema y cómo contribuir sin romper invariantes de negocio.

---
## Índice
1. Resumen Arquitectónico
2. Estructura de Paquetes
3. Modelo de Datos / Entidades
4. Ciclo de Vida de un Pago y Estados
5. Flujo de Autenticación y Autorización (JWT)
6. Flujo: Registro de Admin
7. Flujo: Cambio de Contraseña Propia
8. Flujo: Recuperación de Contraseña (Forgot / Reset)
9. Jobs Programados (Scheduler)
10. Envío de Emails y Flags
11. Repositorios y Acceso a Datos
12. DTOs y Mapeo (Entrada/Salida)
13. Manejo de Errores y Excepciones
14. Validaciones y Reglas
15. Transacciones y Consistencia
16. Rendimiento y Consideraciones de Escalabilidad
17. Seguridad (Checklist + Riesgos)
18. Configuración, Variables y Feature Flags
19. Testing (Actual vs. Deseado)
20. Logging y Observabilidad (Futuro)
21. Extensiones Futuras / Roadmap Técnico Profundo
22. Principios y Estándares de Código
23. Riesgos Técnicos y Mitigaciones
24. Guía de Contribución Rápida

---
## 1. Resumen Arquitectónico
Aplicación monolítica sobre Spring Boot (API REST) + frontend estático simple (HTML/CSS/JS). Seguridad stateless basada en JWT. Persistencia con Spring Data JPA a PostgreSQL. Se privilegia: simplicidad, legibilidad, bajo acoplamiento y fácil evolución.

Patrones clave:
- Stateless API (no sesiones de servidor).
- Materialización de estados (EXPIRED) para consultas más simples.
- Baja lógica en front: la API entrega datos ya interpretables.
- Recuperación de contraseña sin librerías adicionales (tokens en memoria temporal).

---
## 2. Estructura de Paquetes
```
com.gym.gym_management
 ├─ authentication/        (Login, DTOs de auth, emisión de JWT)
 ├─ configuration/         (Seguridad, beans, filtros)
 ├─ controller/            (Endpoints REST y orquestación de servicios)
 ├─ controller/dto/        (DTOs de entrada/salida)
 ├─ job/                   (Jobs programados: vencimientos, recordatorios)
 ├─ model/                 (Entidades JPA, enums, logs de notificación)
 ├─ repository/            (Interfaces JpaRepository y queries específicas)
 ├─ service/               (Lógica de negocio, email, password reset, usuarios)
 └─ ... util (potencial futuro)
```
Principio: cada capa con una responsabilidad estrecha, sin atajos entre controller ↔ repository.

---
## 3. Modelo de Datos / Entidades
### 3.1 Client
- Identidad: `id` (Long).
- Campos: nombre, apellido, email (único), teléfono, notas, isActive, fechas de pausa, razón de pausa.
- Relaciones: 1:N con `Payment`.
- Reglas: baja lógica (isActive=false), se permite pausa temporal con rango.

### 3.2 Payment
- Campos: `paymentDate`, `expirationDate`, `amount`, `method`, `month/year` (cuando es mensual), `durationDays` (modalidad flexible), `paymentState`, flags de anulación (voided, voidReason, voidedAt, voidedBy).
- Responsabilidad: representar una ventana de vigencia pagada.
- Flujo de expiración: job convierte UP_TO_DATE → EXPIRED.

### 3.3 User
- Email único, password (BCrypt), `Role` (por ahora solo ADMIN).
- Implementa `UserDetails` para integrarse con Spring Security.

### 3.4 NotificationLog 
- Propósito: registrar envío de recordatorios (paymentId, tipo, estado, días de anticipación).
- Futura base para mostrar “recordatorio enviado” en UI.

### 3.5 Enumeraciones
- `PaymentState`: UP_TO_DATE | EXPIRED | VOIDED.
- `PaymentMethod`: (depende del enum definido: efectivo, transferencia, etc.).
- `NotificationType`, `NotificationStatus`: para log de notificaciones.

---
## 4. Ciclo de Vida de un Pago y Estados
1. Creación (UP_TO_DATE) → se calcula expirationDate (mes +1 o días personalizados).
2. Anulación (VOIDED) → no se elimina; se conserva auditoría básica.
3. Expiración automática (EXPIRED) por job nocturno.
4. Reportes consultan directamente por `paymentState` (sin recalcular en runtime).

Ventajas de persistir EXPIRED:
- Queries simples (ej. `WHERE payment_state='EXPIRED'`).
- Preparado para índices y optimización.
- Base para escalar métricas/estadísticas.

---
## 5. Flujo de Autenticación y Autorización (JWT)
1. Login: credenciales → validación → emisión JWT (header Authorization: Bearer ...).
2. Cada request: filtro `JwtAuthenticationFilter` extrae token, valida firma y carga `UserDetails`.
3. Autorización: reglas en `SecurityConfiguration` y anotaciones `@PreAuthorize`.
4. Stateless: no hay sesión HTTP; revocación sólo posible por rotación de secret (en MVP).

Puntos de mejora:
- Refresh tokens.
- Lista de revocación (blacklist) o invalidación selectiva.
- Claims adicionales (ej: issued_at, versionado de credenciales).

---
## 6. Flujo: Registro de Admin
1. Admin autenticado abre formulario de nuevos admins.
2. Envía DTO: email + password.
3. Servicio valida unicidad y codifica password con BCrypt.
4. Persiste `User` con rol ADMIN.
5. Se retorna DTO sin exponer password.

Reglas:
- Email único obligatorio.
- Password nunca en texto plano en logs.

---
## 7. Flujo: Cambio de Contraseña Propia
1. Admin autenticado solicita cambio (`POST /api/users/change-password`).
2. Se valida contraseña actual → `passwordEncoder.matches`.
3. Se valida confirmación y complejidad mínima (frente: regex; backend: pendiente robustecer).
4. Se guarda nueva contraseña y opcionalmente se podría forzar re-login (futuro: invalidar tokens previos).

Errores comunes devueltos:
- Actual incorrecta.
- Nueva y confirmación no coinciden.

---
## 8. Flujo: Recuperación de Contraseña (Forgot / Reset)
1. `POST /api/password/request-reset` con email.
2. Si existe: genera token seguro (Base64 URL, 32 bytes) + expiración (30 min) → almacena en mapa en memoria.
3. Envía email con enlace `reset-password.html?token=...`.
4. Front valida token vía `GET /api/password/validate-token/{token}`.
5. Nuevo password enviado a `POST /api/password/confirm-reset`.
6. Se invalida token (remoción del mapa).

Limitaciones actuales:
- Tokens volátiles (se pierden al reiniciar).
- No hay límite de solicitudes por unidad de tiempo.

Mitigación futura:
- Persistir tokens (tabla `password_reset_token`).
- Rate limiting (Bucket4j / Redis).
- Reuso de token prohibido (ya cubierto al eliminarlo tras uso).

---
## 9. Jobs Programados (Scheduler)
| Job | Propósito | Frecuencia | Idempotencia |
|-----|-----------|------------|--------------|
| `PaymentExpirationJob` | Materializar EXPIRED | Diario (ej: 02:00) | Sí (no repite cambios) |
| `PaymentReminderJob` | Enviar recordatorios próximos a vencer | Diario (09:00) | Controla duplicados usando `NotificationLog` |

Flags influyentes:
- `app.reminder.enabled` controla existencia de bean de recordatorios.
- `app.reminder.daysBefore` ajusta horizonte (ej: 3 días).
- `app.reminder.log` para logging simple.

---
## 10. Envío de Emails y Flags
Servicio: `EmailService`.

Comportamiento:
- Si `app.email.enabled=false` → log en consola (simulación).
- Si `true` → usa `JavaMailSender` y `MimeMessageHelper`.

Tipos de correo actuales:
- Recordatorio de expiración de pago.
- Recuperación de contraseña.

Mejoras futuras:
- Plantillas externas (Thymeleaf / Freemarker) para evitar HTML inline.
- Inyección de variables de branding.
- Configurar pool SMTP / transporte asíncrono.

---
## 11. Repositorios y Acceso a Datos
Uso de `JpaRepository` para CRUD y extensiones con métodos derivados.

Beneficios:
- Reducción de boilerplate.
- Métricas y optimizaciones futuras (añadir índices) sin tocar services.

Buenas prácticas esperadas:
- Evitar lógica de negocio en repos.
- Métodos de consulta nombrados semánticamente (ej: `findByExpirationDateWithClient`).

---
## 12. DTOs y Mapeo (Entrada/Salida)
Principios:
- Nunca exponer entidades JPA directamente al front.
- DTOs alineados a casos de uso específicos (no “mega-DTO”).
- Validaciones declarativas con Bean Validation en DTOs de entrada (`@NotNull`, `@Email`, etc.).

Ejemplos:
- `ChangePasswordDTO`.
- `PasswordResetRequestDTO` / `PasswordResetConfirmDTO`.
- (Futuro) DTO minimal para listado de admins (email, rol).

Mapeo manual vs. librerías:
- MVP: mapeo manual para control explícito y evitar sobrecarga.
- Crecimiento: posible introducir MapStruct.

---
## 13. Manejo de Errores y Excepciones
Objetivo: respuestas claras y consistentes.

Estrategia actual:
- Control granular de errores en controllers (ej: `IllegalArgumentException`).
- Recomendado (si no existente): `@ControllerAdvice` global para mapear excepciones conocidas a HTTP codes:
  - Validación: 400.
  - Entidad no encontrada: 404.
  - Violación de negocio: 422.
  - Error genérico: 500.

Futuro:
- Estructura estándar JSON: `{ timestamp, path, errorCode, message, correlationId }`.

---
## 14. Validaciones y Reglas
- Bean Validation en DTOs.
- Reglas de dominio:
  - Un pago requiere cliente activo (en registros futuros se puede reforzar).
  - Cambio de contraseña: confirmar y validar actual.
  - Recuperación: token válido y no expirado.

Pendientes:
- Política robusta de contraseña (backend) → longitud, complejidad, blacklist.
- Evitar reutilización inmediata (historial de contraseñas).

---
## 15. Transacciones y Consistencia
- Operaciones de escritura principales: anotadas con `@Transactional` (implícito en métodos repository + services si se añade).
- Jobs: transaccionales para garantizar atomicidad de actualizaciones en lotes.
- Estrategia de consistencia: fuerte (commit inmediato sobre PostgreSQL).

Posibles mejoras:
- Transacciones de solo lectura (`@Transactional(readOnly=true)`) para consultas pesadas.
- Propagación explícita si se añaden orquestaciones complejas.

---
## 16. Rendimiento y Consideraciones de Escalabilidad
Actual:
- Dominio pequeño, queries simples.
- Carga base en tiempo de arranque (seed).

Posibles cuellos de botella futuros:
- Listas sin paginación (p.ej. clientes, pagos masivos).
- Envío de emails síncrono dentro del job (bloquea el thread del scheduler).
- Falta de índices en columnas de filtro frecuente (`payment_state`, `expirationDate`).

Mitigaciones:
- Añadir paginación (`Pageable`) en endpoints de listados.
- Mover emails a cola (JMS / Kafka / RabbitMQ) o `@Async`.
- Revisar plan de ejecución e índices (EXPLAIN ANALYZE) cuando escale.

---
## 17. Seguridad (Checklist + Riesgos)
Checklist actual:
- [x] Hash de contraseñas (BCrypt)
- [x] JWT stateless
- [x] CSRF deshabilitado (API token-based)
- [x] Endpoints públicos mínimos (reset password + estáticos)
- [x] Rate limiting (login / password reset)  <!-- Actualizado: ahora implementado -->
- [ ] Rotación de secret JWT
- [ ] Persistencia y revocación de tokens de recuperación
- [ ] Auditoría de eventos (login, reset, cambio password)

Riesgos actuales:
- Brute force sobre login (mitigado parcialmente con rate limiting in-memory; falta solución distribuida)
- Enumeración de emails (mitigado con respuestas genéricas y conteo de intentos en reset)

### 17.1 Rate Limiting (Implementación Actual)
Se añadió un servicio `RateLimitService` en memoria que aplica límites por combinación `email + IP`:
- Login: por defecto 5 intentos fallidos → bloqueo temporal (10 min configurable).
- Password reset: 5 solicitudes consecutivas → bloqueo temporal (15 min configurable).
- Tras un login exitoso se resetea el contador de ese par email/IP.
- Los intentos de login con body inválido también cuentan para reducir vectores de enumeración.

Limitaciones:
- Estado no distribuido (se pierde al reiniciar y no escala horizontalmente).
- IP obtenida vía `HttpServletRequest#getRemoteAddr` (no considera proxy / X-Forwarded-For).

Mejoras futuras sugeridas:
1. Reemplazar por Bucket4j + Redis para despliegues multi-nodo.
2. Añadir cabecera configurable (X-Forwarded-For) y validación de lista de proxies de confianza.
3. Telemetría de bloqueos (contador Prometheus) para monitoreo y alertas.
4. Backoff exponencial dinámico según historial (ej.: ventana deslizante adaptativa).

---
## 18. Configuración, Variables y Feature Flags
| Clave | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `SPRING_DATASOURCE_URL` | env | URL de BD | jdbc:postgresql://localhost:5432/gym_management |
| `SPRING_DATASOURCE_USERNAME` | env | Usuario BD | postgres |
| `SPRING_DATASOURCE_PASSWORD` | env | Password BD | postgres |
| `JWT_SECRET` | env | Secreto firma JWT (Base64) | (cadena >=32 bytes) |
| `app.reminder.enabled` | prop | Activa job de recordatorios | true |
| `app.reminder.daysBefore` | prop | Días de anticipación | 3 |
| `app.reminder.log` | prop | Logging simple en job | false |
| `app.email.enabled` | prop | Habilita envío real | false |
| `app.mail.from` | prop | Remitente | no-reply@gym.com |
| `security.ratelimit.login.maxAttempts` | prop | Intentos fallidos de login antes de bloquear | 5 |
| `security.ratelimit.login.blockMinutes` | prop | Minutos de bloqueo tras exceder intentos login | 10 |
| `security.ratelimit.passwordReset.maxAttempts` | prop | Solicitudes de reset antes de bloquear | 5 |
| `security.ratelimit.passwordReset.blockMinutes` | prop | Minutos de bloqueo tras exceder solicitudes reset | 15 |

Notas de operación:
- Ajustar límites según monitoreo real (ej. entornos con más fallos benignos pueden subir a 7–8).
- Si se pasa a un enfoque distribuido, mantener mismas claves para evitar ruptura.

---
## 19. Testing (Actual vs. Deseado)
Estado actual (parcial):
- Test de arranque (smoke) `GymManagementApplicationTests`.
- Algún test puntual de `UserService`.
- Test de integración simple para `PasswordResetService` (mock email).

Gaps:
- Sin tests para jobs.
- Sin tests de controladores (WebMvcTest) con validaciones.

Plan incremental:
1. Tests unitarios: `PasswordResetService`, `PaymentReminderJob` (mock repos/email).
2. Tests Web (MockMvc) para endpoints críticos.
3. Test de expiración (marcar pago como EXPIRED con datos semilla controlados).
4. Test de seguridad: acceso denegado sin JWT.

---
## 20. Logging y Observabilidad (Futuro)
Actual:
- Logs simples (info/error). EmailService loguea si modo simulado.

Futuro:
- Estructurar logs JSON para ingestión (ELK / Loki).
- Métricas: contador de pagos procesados, expiraciones, recordatorios enviados.
- `@Timed` en jobs / endpoints (Micrometer + Prometheus).
- Correlation ID en cada request (MDC).

---
## 21. Extensiones Futuras / Roadmap Técnico Profundo
| Prioridad | Feature | Descripción Técnica |
|-----------|---------|---------------------|
| Alta | Persistir tokens reset | Tabla con TTL y limpieza automática |
| Alta | Rate limiting | Bucket4j con caché in-memory o Redis |
| Media | Auditoría | Tabla `security_event` (actor, acción, timestamp, metadata) |
| Media | Paginación | Pageable en listados masivos |
| Media | Recordatorio multi-frecuencia | 7/3/1 días y reintentos fallidos |
| Media | Visualización de notificaciones | Integrar `NotificationLog` en UI (checkbox) |
| Baja | Exportaciones | CSV/PDF backend streaming |
| Baja | Integración correo transaccional | Adaptador SendGrid/SES | 
| Baja | Multi-rol | Roles secundarios (LECTURA limitada) |

---
## 22. Principios y Estándares de Código
- Métodos cortos y descriptivos.
- DTOs para todo tráfico externo.
- Sin strings mágicos: constantes cuando aplica.
- Comentarios JavaDoc en clases y métodos públicos relevantes.
- Evitar usar excepciones genéricas para control de flujo.
- Nombres en PascalCase (clases) y camelCase (variables).
- Evitar efectos secundarios ocultos.

---
## 23. Riesgos Técnicos y Mitigaciones
| Riesgo | Impacto | Mitigación propuesta |
|--------|---------|----------------------|
| Tokens reset en memoria | Pérdida de enlaces tras reinicio | Persistir + TTL + limpieza |
| Falta rate limiting | Ataques fuerza bruta | Implementar Bucket4j / filtro custom |
| Sin auditoría | Dificultad forense | Tabla eventos seguridad + MDC |
| Emails síncronos | Job lento si muchos correos | Async / cola externa |
| Crecimiento de pagos | Queries lentas | Índices + paginación + partición posible |

---
## 24. Guía de Contribución Rápida
1. Crear rama feature: `feature/<descripcion-corta>`.
2. Añadir/actualizar tests si cambias lógica pública.
3. Mantener estilo existentes (sin introducir frameworks innecesarios).
4. Documentar nuevas propiedades en sección Configuración.
5. Ejecutar build local: `./mvnw clean test`.
6. Crear PR describiendo: objetivo, cambios clave, impacto en seguridad y datos.

---
## Apéndice A: Ejemplo de Flujo de Expiración
1. El pago se crea con `expirationDate` calculada.
2. A las 02:00 el job busca pagos UP_TO_DATE con `expirationDate < hoy`.
3. Actualiza `paymentState=EXPIRED` en lote.
4. Reportes consultan directamente por estado.

## Apéndice B: Notas sobre Indización (Futuro)
Sugeridos:
- Índice compuesto: `(payment_state, expiration_date)` para reportes por estado + rango.
- Índice en `client.email` (único ya implícito por constraint).

## Apéndice C: Politica de Complejidad de Password (Sugerida)
- Longitud mínima: 10.
- Al menos 1 mayúscula, 1 minúscula, 1 número, 1 símbolo.
- No contener más del 50% del email local-part.

---
Si necesitas más detalle, puedes ampliar este documento o abrir tareas específicas en el repositorio.
