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

## Notas de diseño

- Modelo principal: `Client` y `Payment`.
  - `Payment` tiene `paymentDate`, `expirationDate`, `state`, `voided` y `durationDays` (opcional) para pagos por días.
  - Si `durationDays` no viene, el sistema asume 1 mes de vigencia.
- API expone DTOs (no devuelve entidades JPA directamente).
- Seguridad con JWT (rol ADMIN para panel).

---

## Roadmap (cosas para mejorar pronto)

- Mejorar la UI y hacerla responsive.
- Integrar email (notificaciones de pago, bienvenida, etc).
- Filtros avanzados y paginación del lado del servidor.
- Tests de integración adicionales y dockerización para levantar todo con un comando.

---

Hecho con ganas de aprender y construir mi portafolio.
