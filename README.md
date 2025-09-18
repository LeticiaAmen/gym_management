# Gym Management

Proyecto personal de aprendizaje para mi portafolio. Es una app simple para administrar socios y pagos de un gimnasio.

- Diseñé la base de datos y el modelo de backend con Spring Boot.
- Para la parte visual (HTML/CSS/JS) me apoyo en GitHub Copilot para acelerar el front.

---

## ¿Qué hace?

- Clientes: alta/edición/activación y pausa de suscripciones.
- Pagos: registrar, listar, filtrar y anular.
  - La tabla de pagos muestra una columna "Tiempo" con: "1 mes" o "N días" + el rango de vigencia (fecha de pago → fecha de expiración).
- Reportes: membresías vencidas, por vencer (7 días) e ingresos del mes.
- Dashboard: tarjetas con métricas y actividad reciente.

Semilla de datos incluida (data.sql): crea 30 socios con pagos variados; al menos 10 vencidos para probar escenarios reales.

---

## Stack

- Java 17 + Spring Boot
- Spring Web, Spring Security (JWT), Spring Data JPA
- PostgreSQL
- Frontend estático simple (HTML/CSS/JS) en `/src/main/resources/static`

---

## Correrlo en local (rápido)

1) Variables de entorno mínimas (puedes exportarlas en tu terminal):

- `SPRING_DATASOURCE_URL` (ej: `jdbc:postgresql://localhost:5432/gym_management`)
- `SPRING_DATASOURCE_USERNAME` (ej: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (ej: `postgres`)
- `JWT_SECRET` (32 bytes en Base64; para pruebas puedes usar cualquier cadena Base64)

2) Arrancar la app con Maven Wrapper:

```bash
./mvnw spring-boot:run
```

La app crea el esquema y carga la semilla automáticamente (modo demo). Si usas Windows, puedes correr `mvnw.cmd spring-boot:run`.

---

## Login de demo

- Usuario: `admin@gym.com`
- Clave: `password`

(Se crea desde `data.sql` sólo para desarrollo.)

---

## Rutas útiles

- Panel web: `http://localhost:8080/admin/dashboard.html`
- Login: `http://localhost:8080/index.html`

---

## Estrategia de estados de pago

El sistema persiste estados en la entidad `Payment` para simplificar reportes y futuras integraciones (ej: torniquetes o control de acceso).

Estados:
- **UP_TO_DATE**: pago vigente desde `paymentDate` hasta `expirationDate`.
- **EXPIRED**: se materializa (persistido) cuando `expirationDate < hoy` mediante un job diario (`PaymentExpirationJob`). Garantiza consultas rápidas y métricas consistentes.
- **VOIDED**: pago anulado (baja lógica) con motivo y auditoría.

No existe estado PENDING: mientras el pago esté dentro de su vigencia se considera UP_TO_DATE. Si un período aún no tiene pago y ya pasó la fecha conceptual + días de gracia, se considera EXPIRED a nivel lógico (para búsquedas de clientes) aunque no haya registro nuevo.

Job de expiración:
- Corre a las 02:00 AM (`PaymentExpirationJob`) y hace un bulk update UP_TO_DATE → EXPIRED para todos los pagos vencidos.
- Idempotente: múltiples ejecuciones en el día no afectan una vez que se actualizó.

Recordatorios:
- `PaymentReminderJob` (configurable) busca pagos UP_TO_DATE que vencerán en X días y dispara emails con `EmailService`.
- Se pueden desactivar con `app.reminder.enabled=false` (útil en tests).

Ventajas de materializar EXPIRED:
- Índices y queries simples por estado.
- Menos lógica duplicada en frontend.
- Base para reglas futuras (bloqueos, escalado de notificaciones, recargos).

---

## Notas de diseño

- Modelo principal: `Client` y `Payment`.
  - `Payment` tiene `paymentDate`, `expirationDate`, `paymentState`, `voided` y `durationDays` (para pagos por días).
  - `paymentState` se actualiza por lógica de negocio (UP_TO_DATE / EXPIRED / VOIDED).
- API expone DTOs (no devuelve entidades JPA directamente).
- Seguridad con JWT (rol ADMIN para panel).

---

## Roadmap (próximos pasos sugeridos)

- UI responsive.
- Integrar servicio de email transaccional real (SendGrid / SES) con plantillas HTML.
- Control de acceso físico (usar estado EXPIRED materializado).
- Historial de transiciones de estado (tabla `payment_state_history`).
- Docker Compose para levantar DB + app fácilmente.

---

Hecho con ganas de aprender y construir mi portafolio.
