# Gym Management – Backend (Spring Boot)

Una guía rápida para clonar, configurar y ejecutar el proyecto en cualquier entorno sin exponer credenciales.

---

## 🗂️ Índice

1. [Requisitos](#requisitos)
2. [Clonado del repositorio](#clonado-del-repositorio)
3. [Configuración de credenciales](#configuración-de-credenciales)

    * Variables de entorno
    * `application-dev.properties` (local ignorado)
4. [Ejecución](#ejecución)

    * Desde IntelliJ / IDE
    * Desde consola (Maven Wrapper)
5. [Scripts útiles de Maven](#scripts-útiles-de-maven)
6. [Deploy & CI/CD](#deploy--cicd)
7. [Resolución de problemas comunes](#resolución-de-problemas-comunes)

---

## Requisitos

| Herramienta    | Versión mínima                    | Notas                                        |
| -------------- | --------------------------------- | -------------------------------------------- |
| **Java**       | 17                                | Asegúrate de que `java -version` lo devuelva |
| **Maven**      | **💡 Opcional** (usamos `./mvnw`) | El wrapper descarga la versión correcta      |
| **PostgreSQL** | 13+                               | Corriendo localmente o URL remota            |
| **Git**        | Cualquiera                        | Para clonar el repo                          |

> **TIP:** En Windows **PowerShell** agrega *Git Bash* o *WSL* si prefieres comandos *nix*.

---

## Clonado del repositorio

```bash
git clone https://github.com/<tu-org>/gym_management.git
cd gym_management/gymManagement
```

> El sub‑módulo `gymManagement` contiene el backend Spring Boot.

---

## Configuración de credenciales

### 1️⃣ Variables de entorno (todos los entornos)

| Variable                     | Descripción                             |
| ---------------------------- | --------------------------------------- |
| `SPRING_DATASOURCE_URL`      | URL JDBC completa                       |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base                      |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base                   |
| `JWT_SECRET`                 | Clave Base64 (32 bytes) para firmar JWT |

#### Ejemplo – Linux / macOS

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gym_management
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 32)
```

#### Ejemplo – Windows PowerShell

```powershell
$Env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/gym_management"
$Env:SPRING_DATASOURCE_USERNAME = "postgres"
$Env:SPRING_DATASOURCE_PASSWORD = "postgres"
$Env:JWT_SECRET = "<cadena base64>"
```

### 2️⃣ Archivo local ignorado `application-dev.properties`

**Solo para desarrollo local**. No se sube al repo; está listado en `.gitignore`.

Ruta: `src/main/resources/application-dev.properties`

```properties
# Base de datos local
audioMT spring.datasource.url=jdbc:postgresql://localhost:5432/gym_management
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT (32 bytes en Base64)
jwt.secret=(32 bytes en Base64)
```

> Spring carga este archivo automáticamente gracias a la línea:
> `spring.config.import=optional:classpath:application-dev.properties`
> presente en `application.properties`.

---

## Ejecución

### Desde IntelliJ / VS Code

1. Cargar las variables de entorno en la **Run Configuration** o usar `application-dev.properties`.
2. Ejecutar la configuración **Spring Boot** que el IDE genera.

### Desde consola

```bash
# Ejecuta la app con Maven Wrapper
dos2unix ./mvnw  # solo primera vez si estás en Windows + WSL
a./mvnw spring-boot:run
```

---

## Scripts útiles de Maven

| Comando                  | Descripción                                |
| ------------------------ | ------------------------------------------ |
| `./mvnw clean test`      | Limpia y ejecuta tests                     |
| `./mvnw package`         | Genera `gym_management-0.0.1-SNAPSHOT.jar` |
| `java -jar target/*.jar` | Corre el jar empacado                      |

---

## Deploy & CI/CD

1. Definir **JWT\_SECRET** y credenciales de base en los *Secrets* del proveedor (GitHub Actions, Railway, Heroku…).
2. Compilar: `./mvnw package -DskipTests`.
3. Desplegar el jar o usar imagen Docker (próximamente en `/docker`).

---

## Resolución de problemas comunes

| Error                                        | Causa probable                      | Fix rápido                                                           |
| -------------------------------------------- | ----------------------------------- | -------------------------------------------------------------------- |
| `Could not resolve placeholder 'JWT_SECRET'` | No se definió la variable           | Exportar `JWT_SECRET` o rellenar `application-dev.properties`        |
| `MalformedInputException` al copiar recursos | Maven lee archivos en CP‑1252       | Ya solucionado en `pom.xml` con UTF‑8 o cambia encoding              |
| `org.postgresql.util.PSQLException`          | DB apagada o credenciales inválidas | Verificar que PostgreSQL esté corriendo y los valores sean correctos |

---

> Hecho con ❤️ por Leticia Amen
