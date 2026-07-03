# Prueba Técnica — Gestión de Usuarios (Java 21 · Spring Boot · PostgreSQL · ZK)

Aplicación web de administración de usuarios construida sobre una **librería de autenticación reutilizable**, con arquitectura Maven multi-módulo, Clean Architecture y verificación automática de las reglas de capas.

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.3 |
| UI | ZK 10 (patrón MVVM) vía `zkspringboot-starter` |
| Persistencia | Spring Data JPA + PostgreSQL, migraciones con Flyway |
| Seguridad | Spring Security + BCrypt |
| Tests | JUnit 5, Mockito, AssertJ, ArchUnit (42 tests) |

---

## 1. Arquitectura

### Módulos

```
prueba-tecnica (pom padre)
├── user-auth-library    → librería independiente de autenticación y usuarios (jar)
└── web-app              → aplicación web ZK que la consume (jar ejecutable, Tomcat embebido)
```

La regla de dependencia es unidireccional: `web-app → user-auth-library`, nunca al revés. La librería no sabe que existe una interfaz ZK; podría consumirla igual un API REST o una aplicación de escritorio.

### Capas dentro de la librería (Clean Architecture)

```
        presentation (web-app)          ─┐
              │ usa puertos              │  las flechas SIEMPRE
              ▼                          │  apuntan hacia dentro
 ┌── application (casos de uso, DTOs, puertos in/out)
 │            │
 │            ▼
 │        domain (agregado User, value objects, reglas de negocio)
 │            ▲
 └── infrastructure (JPA, BCrypt) — implementa los puertos de salida
```

- **`domain/`** — Java puro, sin ninguna dependencia de framework. Agregado `User` con métodos de negocio (no setters), value objects *always-valid* (`Username`, `Email`, `PasswordHash`), excepciones de negocio.
- **`application/`** — casos de uso tras puertos de entrada (`UserService`, `Authenticator`), puertos de salida que la infraestructura implementa (`UserRepositoryPort`, `PasswordEncoderPort`), DTOs inmutables con Bean Validation.
- **`infrastructure/`** — entidad JPA separada del modelo de dominio (con mapper explícito), repositorio Spring Data, adaptador BCrypt.
- **`autoconfigure/`** — la librería se comporta como un starter de Spring Boot: registro explícito de beans con `@ConditionalOnMissingBean` (el consumidor puede sustituir cualquier pieza declarando su propio bean).

**Estas reglas no son documentación: son un test.** `ArchitectureTest` (ArchUnit) rompe el build si el dominio importa Spring/JPA o si la capa de aplicación toca infraestructura.

### Decisiones de diseño que conviene conocer

| Decisión | Motivo |
|---|---|
| `Authenticator` como clase abstracta (Template Method) | El flujo (validar → autenticar → notificar) es fijo; cada mecanismo (`PasswordAuthenticator`, y mañana LDAP/JWT) implementa solo sus pasos. `authenticate()` es `final`: ninguna subclase puede romper el contrato. |
| Entidad JPA separada del agregado de dominio | El dominio queda testeable sin arrancar Spring y libre de anotaciones de persistencia. El precio (un mapper) se paga una vez. |
| Excepciones traducidas en el adaptador | La violación de la constraint UNIQUE (carrera entre dos registros simultáneos) se captura y se convierte en `UserAlreadyExistsException`: el consumidor nunca ve errores técnicos de JDBC. |
| Mensajes de login genéricos | "Credenciales inválidas" tanto para usuario inexistente como contraseña errónea → impide enumerar cuentas. El estado "deshabilitada" solo se revela tras contraseña correcta. |
| Regla del último administrador | El dominio impide desactivar/eliminar al último `ADMIN` habilitado: el sistema no puede quedarse sin administración. |
| Paginación con `PageResult` propio | Los puertos no filtran tipos de Spring Data; la librería es agnóstica de la tecnología de persistencia también en sus contratos. |
| Versión JPA como `Long` (wrapper) | Con IDs asignados por el dominio, Spring Data usa `version == null` para detectar entidades nuevas y evita un SELECT extra por inserción. |

---

## 2. Requisitos previos

| Requisito | Nota |
|---|---|
| **JDK 21** | El proyecto compila con `--release 21`. Si tu `JAVA_HOME` apunta a otro JDK, ver [Solución de problemas](#6-solución-de-problemas). |
| **Maven 3.9+** | |
| **PostgreSQL 14+** | Solo el servidor; DBeaver u otro cliente es opcional para inspeccionar datos. |

No necesitas instalar Tomcat: la aplicación empaqueta Tomcat embebido (jar ejecutable de Spring Boot).

---

## 3. Puesta en marcha

### Paso 1 — Crear la base de datos

Ejecuta [`scripts/01_create_database.sql`](scripts/01_create_database.sql) conectado al servidor como superusuario (por ejemplo desde DBeaver). Crea la base `prueba_tecnica` vacía.

> Las **tablas no se crean a mano**: las crea Flyway automáticamente en el primer arranque, a partir de las migraciones versionadas de la librería (`db/migration/userauth/V1__create_users.sql`).

### Paso 2 — Configurar la conexión

Los valores por defecto son `localhost:5432`, usuario `postgres`, contraseña `postgres`. Si los tuyos difieren, **no edites el yml**: usa variables de entorno (los puntos exactos a cambiar están comentados con ⚠️ en [`web-app/src/main/resources/application.yml`](web-app/src/main/resources/application.yml)):

```powershell
$env:DB_URL      = "jdbc:postgresql://localhost:5432/prueba_tecnica"   # opcional
$env:DB_USERNAME = "postgres"                                          # opcional
$env:DB_PASSWORD = "tu-contraseña"
```

### Paso 3 — Compilar y arrancar

```powershell
mvn clean install            # compila ambos módulos y ejecuta los 42 tests
mvn spring-boot:run -pl web-app
```

### Paso 4 — Entrar

Abre **http://localhost:8080**. Serás redirigido al login.

**Credenciales iniciales** (se crean automáticamente solo si la BD está vacía):

| Usuario | Contraseña |
|---|---|
| `admin` | `Admin1234` |

> ⚠️ Cambia esta contraseña tras el primer inicio de sesión (botón "Mi contraseña") o define `BOOTSTRAP_ADMIN_PASSWORD` antes del primer arranque.

Para verificar el estado de la base de datos tras el arranque (migraciones, hash BCrypt, roles), ejecuta [`scripts/02_consultas_verificacion.sql`](scripts/02_consultas_verificacion.sql) en DBeaver.

---

## 4. Funcionalidad

- **Login** con la librería propia (`PasswordAuthenticator`) integrado en la sesión de Spring Security. Todas las páginas requieren autenticación; el CRUD exige rol `ADMIN`.
- **Listado de usuarios**: búsqueda por usuario/correo, paginación en servidor (10 por página), estado visual activo/inactivo.
- **Crear usuario**: validación en dos niveles (Bean Validation en frontera + value objects en dominio), política de contraseña fuerte (`@StrongPassword`), roles USER/ADMIN.
- **Restablecer contraseña** (acción de administrador, no requiere la actual) y **cambiar mi contraseña** (autoservicio, requiere la actual) como casos de uso separados.
- **Activar/desactivar y eliminar** con confirmación, guardas de cuenta propia en la UI y regla del último administrador en el dominio.

## 5. Tests

```powershell
mvn test
```

42 tests, sin necesidad de base de datos ni contexto de Spring:

- **Dominio**: invariantes de `Username`/`Email`, comportamiento del agregado `User`.
- **`AuthenticatorTest`**: contrato del método plantilla — nunca devuelve `null`, nunca propaga excepciones de negocio, los hooks no pueden alterar el desenlace.
- **`PasswordAuthenticatorTest`**: credenciales erróneas/cuenta deshabilitada/entrada malformada, con verificación de los mensajes anti-enumeración.
- **`DefaultUserServiceTest`** (Mockito): unicidad, cambio/reset de contraseña, protección del último administrador.
- **`ArchitectureTest`** (ArchUnit): reglas de dependencia entre capas.

## 6. Solución de problemas

| Síntoma | Causa y solución |
|---|---|
| `release version 21 not supported` o `invalid target release` | Maven está usando un JDK < 21. Apunta `JAVA_HOME` a un JDK 21 antes de ejecutar Maven. En esta máquina hay uno portable: `$env:JAVA_HOME = "C:\Users\Andres\.jdks\jdk-21.0.11+10"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"` |
| `FATAL: password authentication failed` | Usuario/contraseña de PostgreSQL incorrectos: define `DB_USERNAME`/`DB_PASSWORD` (paso 2). |
| `FATAL: database "prueba_tecnica" does not exist` | Falta el paso 1 (`scripts/01_create_database.sql`). |
| `Port 8080 was already in use` | Otro proceso ocupa el puerto: `$env:SERVER_PORT=8081` o libera el 8080. |
| El login no acepta `admin`/`Admin1234` | El admin solo se crea si la tabla `users` está **vacía** en el arranque. Verifica con el script 02; si hay datos corruptos de pruebas, vacía las tablas y reinicia. |

## 7. Estructura del proyecto

```
├── pom.xml                                  # POM padre: BOM Spring Boot, Java 21, versiones
├── scripts/                                 # SQL manual: crear BD y consultas de verificación
├── user-auth-library/
│   └── src/main/java/com/acme/userauth/
│       ├── domain/          (model, vo, exception)          # núcleo, sin frameworks
│       ├── application/     (port/in, port/out, service, dto, validation)
│       ├── infrastructure/  (persistence, security)         # JPA, BCrypt
│       └── autoconfigure/                                   # starter Spring Boot
│   └── src/main/resources/db/migration/userauth/            # migraciones Flyway (V1–V99)
│   └── src/test/java/                                       # 42 tests unitarios + ArchUnit
└── web-app/
    └── src/main/java/com/acme/webapp/
        ├── presentation/    (viewmodel, model, zk)           # MVVM de ZK
        ├── infrastructure/security/                          # puente librería ↔ Spring Security
        └── config/                                           # SecurityConfig, AdminBootstrap
    └── src/main/resources/
        ├── web/zul/         (auth/, user/)                   # vistas ZK declarativas
        ├── static/css/app.css
        └── application.yml                                   # ⚠️ conexión BD comentada aquí
```
