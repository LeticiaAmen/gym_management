# Gym Management

App web para quienes administran un gimnasio y quieren dejar de depender de planillas dispersas. Te ayuda a saber quién está al día, quién vence pronto, qué pagos registrar y cómo mantener un orden simple sin sobrecarga.

Todo corre en tu propio entorno: tú controlas la base de datos y los datos no salen de tu infraestructura. Ideal para practicar, iterar e incluso extender según tus necesidades.

> ¿Buscas detalles técnicos (arquitectura, seguridad, estados, roadmap profundo)? Están en: [`docs/technical-overview.md`](./docs/technical-overview.md)

---

## ¿Qué hace?

- Dashboard en un vistazo: métricas de clientes, pagos recientes y actividad relevante.
- Clientes organizados: alta, edición, pausa / reactivación y seguimiento del estado.
- Pagos claros: registrar, anular (baja lógica), ver vigencias y próximos vencimientos.
- Reportes útiles: membresías vencidas, por vencer (7 días) e ingresos del mes.
- Recordatorios por email: avisa antes de que un pago expire (opcional por flag).
- Múltiples administradores: registra otros admins de forma segura (password BCrypt).
- Cambio de contraseña personal: cada admin gestiona la propia, no la de otros.
- Recuperación de acceso: flujo de “olvidé mi contraseña” con token temporal.

Semilla incluida (`data.sql`): crea clientes y pagos variados (vigentes, próximos a vencer y vencidos) para explorar sin cargar datos manualmente.

---

## Cómo empezar rápido

1. Instala Java 17 (si aún no lo tienes).
2. Asegura que tienes PostgreSQL funcionando y una base creada (vacía) para pruebas.
3. Exporta/define variables de entorno mínimas:
   - `SPRING_DATASOURCE_URL` (ej: `jdbc:postgresql://localhost:5432/gym_management`)
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET` (cualquier cadena Base64 de al menos 32 bytes en desarrollo)
4. Ejecuta la app con el wrapper de Maven.
5. Abre el navegador y entra al panel.
6. Empieza a explorar: crea pagos, pausa un cliente, genera reportes.

Credenciales demo:
- Usuario: `admin@gym.com`
- Clave: `password`

> Nota: sólo para desarrollo. Cámbialo en entornos reales.

---

## Instalación local paso a paso

Linux / macOS:
```bash
# 1) Clonar (o descarga el ZIP y descomprime)
git clone <url-del-repo>
cd gymManagement

# 2) Ejecutar la aplicación (compila + levanta servidor)
./mvnw spring-boot:run
```
Windows (CMD o PowerShell):
```bat
REM 1) Clonar
git clone <url-del-repo>
cd gymManagement

REM 2) Ejecutar
mvnw.cmd spring-boot:run
```
La app levantará (por defecto) en: `http://localhost:8080/`

---

## Navegación principal

- Dashboard: resumen general.
- Clientes: administración de socios (estado, pausas, detalle rápido).
- Pagos: registro, búsqueda y control de vigencias.
- Reportes: vencidos, por vencer y resumen de ingresos.
- Administradores: alta de nuevos admins y listado.
- Cambiar contraseña: formulario interno (solo propia cuenta).
- Recuperar contraseña: enlaces públicos `forgot-password.html` y `reset-password.html`.

---

## Recuperación de contraseña (flujo amigable)

1. El admin solicita desde `Olvidaste tu contraseña?`.
2. Si el email existe, se envía un enlace con token (30 min de validez).
3. Abre el enlace → establece nueva contraseña.
4. El token se invalida al usarse.

Siempre se responde con mensaje genérico para no filtrar existencia de correos.

---

## Configuración rápida

Variables / flags útiles:
- `JWT_SECRET`: firma de los tokens.
- `app.reminder.enabled=true|false`: activa job de recordatorios.
- `app.email.enabled=true|false`: si es false, se loguea el correo en consola.
- `app.mail.from`: remitente mostrado en emails.

Mientras `app.email.enabled=false` puedes probar el flujo sin un servidor SMTP real.

---

## Seguridad y consideraciones

- Este proyecto usa PostgreSQL: tus datos persisten mientras no borres la base.
- La semilla (`data.sql`) es solo para pruebas; elimínala o ajusta para producción.
- Las contraseñas se almacenan con BCrypt.
- Los tokens de recuperación se guardan en memoria (se pierden al reiniciar). Para algo serio: persístelos en BD.

---

## Sigue construyendo (ideas)

- Añadir vista responsive para móviles.
- Historial de cambios y auditoría de seguridad.
- Persistir tokens de recuperación y logs de recordatorios.
- Exportar reportes a CSV / PDF.
- Ver indicador “recordatorio enviado” en reportes de vencimientos.
- Integrar un servicio de correo transaccional real.

Si quieres más profundidad técnica, revisa: [`docs/technical-overview.md`](./docs/technical-overview.md)

---

## Autor

Proyecto creado para aprendizaje y portfolio personal. Si te resulta útil o quieres sugerir mejoras, siéntete libre de abrir un issue o forkarlo.

