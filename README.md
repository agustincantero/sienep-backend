# PDI Tercer Semestre DZA Grupo 1 – SIENEP

**Sistema Integral de Estudiantes con Necesidades Educativas Personalizadas**

Este repositorio contiene el backend (API REST) de **SIENEP**, desarrollado en **Java (Spring Boot)** con **Apache Maven**, que utiliza **PostgreSQL** para la persistencia de datos.

Forma parte del **Proyecto de Desarrollo e Infraestructura** del tercer semestre de la **Licenciatura en Tecnologías de la Información** en **UTEC**.

**SIENEP** centraliza y asegura la gestión educativa de **UTEC**, permitiendo al equipo institucional administrar la información de usuarios (funcionarios y estudiantes), realizar un seguimiento exhaustivo mediante instancias y recordatorios, gestionar incidencias y exportar reportes detallados, todo respaldado por un robusto sistema de roles y permisos.

## Tabla de contenidos

- [Requisitos previos](#requisitos-previos)
- [Funcionalidad principal](#funcionalidad-principal)
- [Arquitectura general](#arquitectura-general)
- [Arquitectura de Seguridad y Autenticación](#arquitectura-de-seguridad-y-autenticación)
    - [Login con Google y con Active Directory](#login-con-google-y-con-active-directory)
    - [Recuperación de contraseña](#recuperación-de-contraseña)
    - [Contraseña temporal (`PENDIENTE_DE_ACTIVACION`)](#contraseña-temporal-pendiente_de_activacion)
    - [Formato de las respuestas de error](#formato-de-las-respuestas-de-error)
- [Archivos subidos](#archivos-subidos)
- [Configuración y Ejecución](#configuración-y-ejecución)
    - [Preparar la base de datos (Docker)](#preparar-la-base-de-datos-docker)
    - [Ejecución desde la Terminal](#ejecución-desde-la-terminal)
    - [Ejecución desde IntelliJ IDEA](#ejecución-desde-intellij-idea)
    - [Ejecución de Pruebas (Tests)](#ejecución-de-pruebas-tests)
- [Integración con Google Calendar](#integración-con-google-calendar)
- [Documentación de la API (Swagger)](#documentación-de-la-api-swagger)

## Requisitos previos

1. **Java Development Kit (JDK) 21** o superior.
2. **Docker y Docker Compose** (para ejecutar la base de datos localmente).
3. **Git** para el control de versiones.
4. **IntelliJ IDEA Ultimate** u otro IDE compatible con Spring Boot.

## Funcionalidad principal

La API expone los servicios correspondientes a los 7 módulos centrales del sistema:

1. **Autenticación y Acceso:** Inicio de sesión con email y contraseña, con Google (`POST /auth/google`) o con Active Directory (`POST /auth/ad`); consulta de la sesión propia (`GET /auth/me`), cierre de sesión que invalida los JWT emitidos antes (`POST /auth/logout`) y gestión segura de credenciales.
2. **Gestión de Estudiantes:** Altas, bajas lógicas, modificaciones, búsquedas y capacidad para adjuntar informes médicos.
3. **Gestión de Instancias:** Registro, categorización, visualización e integración con Google Calendar (RF13), con la opción de crear un recordatorio inicial en la misma llamada. La clonación (RF17) se resuelve con los endpoints estándar de lectura y creación: el Frontend trae los datos de la instancia original con `GET /instancias/{id}`, precarga un formulario editable (fecha, canal, categoría, etc.) y crea la nueva instancia con `POST /instancias`. No existe un endpoint dedicado a "clonar" porque no tendría sentido una copia idéntica sin edición previa por parte del usuario.
4. **Gestión de Recordatorios:** Creación (únicos y recurrentes), notificaciones, vinculación a instancias e integración con Google Calendar.
5. **Gestión de Incidencias:** Registro detallado y visualización del historial de incidencias.
6. **Reportes:** Generación y exportación en PDF de los 7 indicadores del Data Warehouse (instancias e incidencias por mes, instancias por categoría y canal, incidencias por tipo de espacio, día y franja horaria, e involucrados por incidencia), leídos de las vistas del DW (`sienep_dw`) con un usuario de solo lectura. El período es por mes completo (`desde`/`hasta`, por defecto todo lo cargado) y el reporte indica hasta qué fecha llegan los datos (`datosAl`). Son globales a propósito, sin filtro por estudiante, grupo ni carrera. Si el DW no está configurado (`DW_URL`, `DW_USER`, `DW_PASSWORD`) o no responde, el reporte responde 503. Incluye un endpoint (`POST /reportes/validar`, requiere login y el permiso `VER_REPORTES`) para verificar si un PDF exportado es auténtico, comparando su hash SHA-256 contra el que el sistema guardó al generarlo.
7. **Administración y Configuración:** Gestión completa de Roles y Permisos (preexistentes y personalizados), y mantenimiento de las categorías de Instancias y Recordatorios.

## Arquitectura general

Este proyecto sigue una arquitectura en capas estándar de Spring Boot orientada al diseño de APIs RESTful:

- **Controladores (`controller/`):** Exponen los endpoints HTTP, documentados con Swagger (OpenAPI 3), y gestionan las peticiones y respuestas.
- **Servicios (`service/`):** Contienen la lógica de negocio central.
- **Repositorios (`repository/`):** Interfaces de Spring Data JPA para el acceso a PostgreSQL.
- **Modelo (`model/`):** Entidades JPA mapeadas directamente al esquema relacional.
- **DTOs (`dto/`):** Objetos de transferencia de datos. Se utiliza el patrón CQRS simplificado separando las entradas de las salidas para mantener validaciones limpias e independientes.
- **Mapeadores (`mapper/`):** Interfaces procesadas por `MapStruct` para automatizar la conversión bidireccional entre Entidades y DTOs, evitando código repetitivo.
- **Seguridad (`security/`):** Filtros, utilidades de JWT y servicios de extracción de identidad para proteger los endpoints.
- **Configuración (`config/`):** Clases de configuración global (`@Configuration`) para inyectar beans de Spring Security, CORS, OpenAPI, Caché, entre otros.
- **Excepciones (`exception/`):** Manejo centralizado de errores (`@ControllerAdvice`) para capturar fallos y devolver respuestas HTTP estandarizadas al cliente frontend.
- **Utilidades (`utils/`):** Clases sin estado que agrupan lógica reutilizable.
- **Aspectos (`aspect/`):** Implementación de Programación Orientada a Aspectos (AOP) para interceptar de forma transversal la capa de servicios e inyectar el contexto de seguridad web (ID de usuario e IP) en las transacciones de PostgreSQL (`SET LOCAL`), alimentando los triggers de auditoría de la base de datos.

### Patrones de Diseño Implementados

Para asegurar la escalabilidad, el desacoplamiento y la mantenibilidad del código, el equipo aplicó los siguientes patrones de diseño de software:

- **Data Transfer Object (DTO):** Patrón estructural aplicado a lo largo de toda la API que actúa como barrera entre el modelo de datos (Entidades JPA) y la capa de presentación (JSON). Garantiza que la API consuma y exponga únicamente la información estrictamente necesaria, blindando la base de datos de modificaciones directas.
- **Facade (Fachada) - `GoogleCalendarService`:** En lugar de exponer la complejidad del SDK oficial de Google (`ServiceAccountCredentials`, `Calendar.Builder`, y múltiples requests) hacia las capas superiores, esta clase encapsula todo este subsistema engorroso detrás de un único método `procesar(...)` por tipo de evento. Libera a `RecordatorioService`/`InstanciaComunService` de conocer los detalles de la infraestructura externa — solo publican un evento de dominio, sin saber nada del SDK de Google.
- **Builder (Constructor):** Patrón creacional implementado a través de la anotación `@Builder` de Lombok, especialmente en la capa de DTOs y en conjunto con `MapStruct`. Se eligió este patrón para evitar la proliferación de constructores gigantes y asegurar la inmutabilidad en el mapeo de objetos, permitiendo ensamblar instancias complejas paso a paso de forma legible.
- **Singleton (Instancia Única):** Patrón creacional adoptado en toda la capa de lógica y acceso a datos (`@Service`, `@Repository`, `@RestController`). En lugar de instanciar manualmente objetos pesados (que agotarían la memoria), el equipo delegó la gestión del patrón Singleton al Contenedor de Inversión de Control (IoC) de Spring Boot. Spring garantiza que exista una única instancia global de estos componentes en memoria, inyectándolos dinámicamente donde se necesiten.


## Arquitectura de Seguridad y Autenticación

El sistema implementa una arquitectura de seguridad robusta y altamente personalizada basada en **Spring Security** y **JWT** (JSON Web Tokens), operando de forma 100% *stateless* (sin almacenamiento de sesiones en el servidor).

A continuación se detalla la responsabilidad de las clases clave que componen este flujo, un aspecto fundamental del diseño del sistema:

*   **`SecurityConfig`:** Configura el comportamiento central de Spring Security. Deshabilita CSRF (apropiado para APIs REST con JWT), establece la política de sesión sin estado (`STATELESS`), define qué endpoints son públicos (`/auth/**` salvo `GET /auth/me`, Swagger, `/actuator/health` y `/error`) y exige autenticación para el resto. También registra el filtro JWT para que actúe antes de los filtros estándar.
*   **`AuthController` y `AuthService`:** Exponen y procesan `/auth/login`, `/auth/google` y `/auth/ad`. Gestionan la validación inicial, la protección contra ataques de fuerza bruta (bloqueando cuentas tras 5 intentos fallidos en 10 minutos) y auditan el acceso exitoso o denegado. Si las credenciales son válidas, solicitan la emisión del Token.
*   **`LoginUsuarioDetailsService`:** Implementación de `UserDetailsService`. Su única función es asistir en el proceso de **Autenticación (Login)**. Busca al usuario en la base de datos (sea `Funcionario` o `Estudiante`) utilizando su **correo electrónico**. Esto permite a Spring validar la contraseña provista contra el hash encriptado de la base de datos.
*   **`JwtTokenUtil`:** Clase utilitaria encargada de la criptografía. Al generar el Token, asigna el `idUsuario` (la clave primaria) como el *Subject* (sujeto) en lugar del email, mejorando la privacidad y la eficiencia de consultas futuras. Los tokens tienen una validez configurada (ej. 5 horas) y están firmados con algoritmos seguros.
*   **`JwtRequestFilter`:** Filtro interceptor que evalúa cada petición entrante HTTP. Busca el header `Authorization: Bearer <token>`, extrae el JWT, verifica su validez y expiración, y obtiene el `idUsuario` embebido. Si es válido, inyecta la identidad autenticada en el `SecurityContextHolder`.
*   **`CustomUsuarioDetailsService`:** Utilizado exclusivamente por el `JwtRequestFilter` para la fase de **Autorización**. A diferencia del servicio de Login, esta clase busca al usuario directamente por su **ID** y levanta sus privilegios en memoria:
    *   **Funcionarios:** Convierte el Rol y la lista de permisos (`RolPermiso`) asignados dinámicamente en la base de datos a instancias de `GrantedAuthority` de Spring.
    *   **Estudiantes:** Se resuelven con el mismo código: el rol `ESTUDIANTE` es una fila real de la tabla `Rol` (seedeada) y sus permisos salen de `RolPermiso`, logrando que ambos actores convivan transparentemente bajo el mismo control de acceso basado en anotaciones (`@PreAuthorize`). `RolService` impide editar o desactivar los roles base `ADMINISTRADOR` y `ESTUDIANTE`.

### Login con Google y con Active Directory

Ambos endpoints reutilizan el JWT interno y **solo autentican cuentas ya existentes** (no dan de alta): el email verificado tiene que coincidir con el de un Funcionario o Estudiante activo y no bloqueado.

- **`POST /auth/google`:** recibe el ID token que el frontend obtiene con Google Identity Services y lo valida contra `GOOGLE_OAUTH_CLIENT_ID` (Client ID OAuth "Web application", distinto de la Service Account de Calendar).
- **`POST /auth/ad`:** valida usuario (`sAMAccountName`) y contraseña contra el controlador de dominio y enlaza con la cuenta de SIENEP por el atributo `mail`. Usa `AD_LDAP_URL`, `AD_DOMAIN` y `AD_ROOT_DN`; si falta alguna, o el DC no responde, devuelve `503`. Credenciales rechazadas o sin cuenta: `401`.

### Recuperación de contraseña

Si el usuario olvida su contraseña, el flujo se separa en dos pasos para no aplicar el cambio sin confirmación:

1. **`POST /auth/forgot-password`** (público): recibe un email, genera un token de un solo uso (UUID), guarda su hash SHA-256 con vencimiento de 30 minutos e invalida cualquier token anterior del mismo usuario. Envía por correo un link armado con la variable de entorno `FRONTEND_URL` (ej. `https://tu-frontend/reset-password?token=...`). La contraseña actual no se modifica todavía en este paso.
2. **`POST /auth/reset-password`** (público): recibe el token y la nueva contraseña, valida que no esté vencido ni ya usado, y recién ahí la aplica.

Por diseño, este flujo responde siempre igual (200) exista o no el email, para no permitir enumerar cuentas registradas, y limita a **3 solicitudes cada 15 minutos por email** (`429 Too Many Requests` si se supera), para frenar abuso hacia el correo del usuario o hacia el proveedor de mail.

### Contraseña temporal (`PENDIENTE_DE_ACTIVACION`)

Una cuenta nueva, o una cuya contraseña resetea un administrador, queda en estado `PENDIENTE_DE_ACTIVACION` con una contraseña temporal enviada por mail. Puede iniciar sesión, pero `JwtRequestFilter` le responde **403** en cualquier ruta que no sea:

- `/auth/**` (el filtro corre también en las rutas públicas si llega un Bearer), y
- cambiar su propia contraseña: `PATCH /funcionarios/{id}/contrasenia` o `PATCH /estudiantes/{id}/contrasenia`. Es lo único que la pasa a `ACTIVO`. El reset de un administrador (`POST` al mismo path) **no** está permitido en este estado.

Ese 403 lleva `"code": "PASSWORD_CHANGE_REQUIRED"`, para que el frontend lo distinga de un 403 por falta de permisos sin comparar el texto del `message`:

```json
{
  "timestamp": "2026-08-25T15:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Debe cambiar su contraseña temporal antes de continuar.",
  "path": "/carreras",
  "code": "PASSWORD_CHANGE_REQUIRED"
}
```

Esta whitelist vive en `JwtRequestFilter.esRutaPermitidaPendienteActivacion`, separada de las rutas públicas de `SecurityConfig`: si se cambian las públicas hay que revisar también esa lista (hay un comentario cruzado en ambos lugares).

### Formato de las respuestas de error

Todo error trae `timestamp`, `status`, `error`, `message` y `path`. El texto para mostrar al usuario es siempre **`message`** (en español, tuteo). `error` es la etiqueta HTTP estándar en inglés (`"Forbidden"`) y **no** es para mostrar ni para comparar.

Hay dos orígenes de error, con una diferencia que importa:

| Origen | Cuándo | Campos extra |
|---|---|---|
| `GlobalExceptionHandler` | Errores que salen de un controlador o servicio (validación, 404, 409, etc.) | En validación (400): `detalles`, un mapa `campo → mensaje`. **Nunca** trae `code`. |
| `ApiErrorAttributes` (`/error`) | Errores que un filtro responde antes de llegar al controlador: 401 de `JwtRequestFilter` y del entry point, 403 de contraseña temporal, y requests que rechaza el firewall (ej. `//carreras`, 400) | `code`, **solo** cuando el error lo declara (hoy únicamente `PASSWORD_CHANGE_REQUIRED`). |

Es decir: `code` existe únicamente en errores de filtro, y hoy en uno solo. Un 403 por falta de permisos (que sí sale de `@PreAuthorize`, vía `GlobalExceptionHandler`) no lo trae, y ningún error de validación o de negocio tampoco. Los 5xx siempre devuelven el mensaje genérico `"Ocurrió un error inesperado."`.

## Archivos subidos

El sistema permite adjuntar archivos reales (no URLs externas) en dos flujos:

- **Informes adjuntos de un estudiante:** `POST /informes-adjuntos` (multipart) sube el archivo; `GET /informes-adjuntos/{id}/descargar` lo devuelve, protegido por el permiso `VER_INFORME`.
- **Foto de perfil de un estudiante:** `PUT /estudiantes/{id}/foto` (multipart) sube/reemplaza la foto; `GET /estudiantes/{id}/foto` la devuelve. El propio estudiante puede subir y ver su foto sin necesitar el permiso general de edición/visualización.

En ambos casos:

- Formatos aceptados: PDF, JPG/JPEG, PNG. Tamaño máximo: **5MB**.
- El archivo se valida por su extensión declarada y también por sus "magic bytes" (la firma real de su contenido), para evitar que un archivo ejecutable o HTML renombrado con una extensión falsa sea aceptado y luego servido con un `Content-Type` incorrecto.
- Se guardan en disco bajo la carpeta indicada por la variable de entorno `STORAGE_PATH` (por defecto `uploads`, relativa al directorio de ejecución).
- **Importante para el Frontend:** como la autenticación es JWT por header (no por cookie), el link de descarga no es "clickeable" directo desde el navegador. Hay que hacer un `fetch` incluyendo el header `Authorization: Bearer <token>` y armar un blob con la respuesta, en vez de usar un `<a href>` o `<img src>` plano.

## Configuración y Ejecución

### Preparar la base de datos (Docker)

El proyecto incluye un entorno automatizado. Al iniciar el contenedor por primera vez, ejecutará automáticamente el script `src/main/resources/proyecto_schema.sql` para crear el esquema `proyecto`, las tablas y los datos iniciales.

1. Abre una terminal en la raíz del proyecto.
2. Ejecuta el siguiente comando: `docker compose up -d`

**Comandos útiles:**
* `docker compose ps` - Verifica que `sienep-db` esté corriendo.
* `docker compose stop` - Detiene la BD sin borrar datos.
* `docker compose down -v` - Detiene la BD y **borra los datos físicos** (útil si necesitas que el script SQL se vuelva a cargar desde cero).

Las credenciales y la URL JDBC ya están preconfiguradas en el archivo `application.properties` para conectarse a este contenedor.

### Ejecución desde la Terminal

A continuación se muestra cómo configurar las variables para una sesión de terminal. *(Para una configuración permanente, consulte la documentación de su sistema operativo).*

**Paso 1: Configurar variables**

> **Obligatorias.** `JWT_SECRET` (32 caracteres o más) y `DATA_ENCRYPTION_KEY` (32 bytes en Base64, se genera con `openssl rand -base64 32`). Cada ambiente tiene las suyas, y si se pierde la de cifrado, los datos confidenciales ya guardados quedan ilegibles.

> **Nota:** además de las variables obligatorias definidas abajo, existen otras opcionales (si no se definen, usan su valor por defecto; la lista completa está en `.env.example`):
> - `FRONTEND_URL`: URL base del frontend, usada para armar el link de "olvidé mi contraseña" (default `http://localhost:3000`).
> - `STORAGE_PATH`: carpeta base donde se guardan los archivos subidos — informes adjuntos y fotos de perfil (default `uploads`).
> - `CORS_ALLOWED_ORIGINS`: orígenes permitidos para CORS, separados por coma y sin barra final (default `http://localhost:3000,https://sienep.app`). El backend solo acepta peticiones `fetch`/AJAX hechas desde un origen de esta lista.
> - `SWAGGER_ENABLED`: expone Swagger UI (default `true`; ponerla en `false` en producción).
> - `SEED_DEMO_USERS`: si vale `true` siembra funcionarios demo con contraseñas hardcodeadas (default `false`; usar solo en desarrollo local).
> - `GOOGLE_OAUTH_CLIENT_ID`: Client ID para `POST /auth/google` (obligatoria en producción).
> - `DW_URL`, `DW_USER`, `DW_PASSWORD`: conexión de solo lectura a `sienep_dw` para los reportes; si falta alguna, responden `503`.
> - `AD_LDAP_URL`, `AD_DOMAIN`, `AD_ROOT_DN`: conexión al Active Directory para `POST /auth/ad`; si falta alguna, ese endpoint responde `503`.
>
> **Sobre Google Calendar:** con `GOOGLE_CALENDAR_ENABLED=false` (el default si no la definís) la sync queda en modo simulado, no hace falta cargar credenciales reales. Si la ponés en `true`, además necesitás `GOOGLE_CALENDAR_CREDENTIALS` con el JSON completo de la service account — no entra cómodo en una sola línea de terminal, ver `.env.example` o cargarla en un archivo `.env` (ya está en `.gitignore`).

*Windows - Opción 1: PowerShell*
```powershell
$env:JWT_SECRET="<cadena-aleatoria-de-32-caracteres-o-mas>"
$env:DB_URL="jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto"
$env:DB_USER="sienep"
$env:DB_PASSWORD="sienep"
$env:DATA_ENCRYPTION_KEY="<clave-base64-de-32-bytes>"
$env:BREVO_API_KEY="tu_api_key_de_brevo"
$env:GOOGLE_CALENDAR_ENABLED="false"
$env:GOOGLE_CALENDAR_CREDENTIALS="<json-completo-de-la-service-account-en-una-sola-linea>"
$env:GOOGLE_CALENDAR_CALENDAR_ID="tu-calendario-id"
$env:GOOGLE_CALENDAR_IMPERSONATE_ACCOUNT="cuenta@tu-workspace.com"
$env:FRONTEND_URL="http://localhost:3000"
$env:STORAGE_PATH="uploads"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000,https://sienep.app"
```

*Windows - Opción 2: Símbolo del sistema (CMD)*
```cmd
set JWT_SECRET=<cadena-aleatoria-de-32-caracteres-o-mas>
set DB_URL=jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto
set DB_USER=sienep
set DB_PASSWORD=sienep
set DATA_ENCRYPTION_KEY=<clave-base64-de-32-bytes>
set BREVO_API_KEY=tu_api_key_de_brevo
set GOOGLE_CALENDAR_ENABLED=false
set GOOGLE_CALENDAR_CREDENTIALS=<json-completo-de-la-service-account-en-una-sola-linea>
set GOOGLE_CALENDAR_CALENDAR_ID=tu-calendario-id
set GOOGLE_CALENDAR_IMPERSONATE_ACCOUNT=cuenta@tu-workspace.com
set FRONTEND_URL=http://localhost:3000
set STORAGE_PATH=uploads
set CORS_ALLOWED_ORIGINS=http://localhost:3000,https://sienep.app
```

*macOS & Linux (bash/zsh)*
```bash
export JWT_SECRET="<cadena-aleatoria-de-32-caracteres-o-mas>"
export DB_URL="jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto"
export DB_USER="sienep"
export DB_PASSWORD="sienep"
export DATA_ENCRYPTION_KEY="<clave-base64-de-32-bytes>"
export BREVO_API_KEY="tu_api_key_de_brevo"
export GOOGLE_CALENDAR_ENABLED="false"
export GOOGLE_CALENDAR_CREDENTIALS="<json-completo-de-la-service-account-en-una-sola-linea>"
export GOOGLE_CALENDAR_CALENDAR_ID="tu-calendario-id"
export GOOGLE_CALENDAR_IMPERSONATE_ACCOUNT="cuenta@tu-workspace.com"
export FRONTEND_URL="http://localhost:3000"
export STORAGE_PATH="uploads"
export CORS_ALLOWED_ORIGINS="http://localhost:3000,https://sienep.app"
```

**Paso 2: Ejecutar el proyecto**

Una vez configuradas las variables en tu terminal, ejecutá el proyecto usando Maven Wrapper:

*   **En Windows:** `.\mvnw spring-boot:run`
*   **En macOS/Linux:** `./mvnw spring-boot:run`

### Ejecución desde IntelliJ IDEA

1. Abrí el proyecto en IntelliJ.
2. Entrá en **Run → Edit Configurations…**.
3. Seleccioná el perfil de Spring Boot llamado **SienepApplication** (IntelliJ lo suele detectar y crear automáticamente).
4. Hacé clic en **Modify options** y luego seleccioná **Environment variables**.
5. En el campo de **Environment variables**, agregá lo siguiente:
```text
   JWT_SECRET=<cadena-aleatoria-de-32-caracteres-o-mas>;DB_URL=jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto;DB_USER=sienep;DB_PASSWORD=sienep;DATA_ENCRYPTION_KEY=<clave-base64-de-32-bytes>;BREVO_API_KEY=tu_api_key_de_brevo;GOOGLE_CALENDAR_ENABLED=false;GOOGLE_CALENDAR_CREDENTIALS=<json-completo-de-la-service-account-en-una-sola-linea>;GOOGLE_CALENDAR_CALENDAR_ID=tu-calendario-id;GOOGLE_CALENDAR_IMPERSONATE_ACCOUNT=cuenta@tu-workspace.com;FRONTEND_URL=http://localhost:3000;STORAGE_PATH=uploads;CORS_ALLOWED_ORIGINS=http://localhost:3000,https://sienep.app
```
*(También podés usar el ícono de carpeta al lado del campo para cargarlas en forma de tabla).*

7. Aplicá los cambios (**Apply → OK**).
8. Ejecutá la aplicación con el botón ▶️.

### Ejecución de Pruebas (Tests)

Este proyecto cuenta con una amplia suite de tests unitarios y de integración para garantizar la calidad del código. Para ejecutarlos de forma automatizada, abrí tu terminal en la raíz del proyecto y corré:

*   **En Windows:** `.\mvnw clean test`
*   **En macOS/Linux:** `./mvnw clean test`

> **Nota:** Los tests utilizan una base de datos en memoria (H2) y las propiedades definidas en `application-test.properties`. No es necesario levantar Docker ni configurar las variables de entorno reales en tu computadora para que la suite se ejecute con éxito.

#### Reporte de Cobertura (JaCoCo)
Al ejecutar los tests mediante Maven (`mvn test`), el proyecto generará automáticamente un reporte de cobertura de código utilizando el plugin **JaCoCo**. 

> **Aclaración Arquitectónica:** Por decisión del equipo, el esfuerzo de *Unit Testing* se centró en la **capa de Servicios** (`service/`), ya que es donde reside el núcleo de la lógica de negocio, reglas de validación y orquestación transaccional. Por este motivo, al leer el reporte se recomienda enfocar el análisis en dicho paquete. Los controladores tienen además tests de contrato HTTP con `MockMvc` (validación, códigos de estado, `@PreAuthorize`).

Para visualizar este reporte interactivo y comprobar la alta cobertura de la lógica de negocio, abrí en tu navegador web el siguiente archivo generado localmente:
👉 `target/site/jacoco/index.html`

## Integración con Google Calendar

El sistema sincroniza recordatorios e instancias con Google Calendar usando una **Service Account con domain-wide delegation**: una única cuenta de servicio impersona a una cuenta de la organización y escribe en un único calendario compartido, invitando por email al funcionario responsable (y al alumno, cuando corresponde). No depende de que cada funcionario autorice nada individualmente.

### Configuración
1. En Google Cloud Console, creá una Service Account (**IAM y administración → Cuentas de servicio**) y generá una clave JSON.
2. Habilitá domain-wide delegation para esa cuenta y autorizá el scope `https://www.googleapis.com/auth/calendar.events` en el admin console de Google Workspace, para el Client ID de la service account. **Importante:** `GOOGLE_CALENDAR_IMPERSONATE_ACCOUNT` tiene que ser una cuenta dentro del **mismo Workspace** que hizo esa autorización (ej. una cuenta propia del equipo) — no funciona con una cuenta de un dominio externo (como `@utec.edu.uy`), aunque los **asistentes invitados** a cada evento sí pueden ser de cualquier dominio.
3. Cargá las variables de entorno (ver `.env.example`): `GOOGLE_CALENDAR_ENABLED=true`, `GOOGLE_CALENDAR_CREDENTIALS` (el JSON completo de la service account, pegado tal cual — nunca un archivo en el repo), `GOOGLE_CALENDAR_CALENDAR_ID` y `GOOGLE_CALENDAR_IMPERSONATE_ACCOUNT`.

Con `GOOGLE_CALENDAR_ENABLED=false` (default), la sync queda en **modo simulado**: loguea la operación y devuelve un id `SIMULADO-...` en vez de llamar a la API real — no hace falta cargar credenciales para desarrollar o correr los tests.

### Sincronización
La sync es asíncrona y corre recién después de confirmada la transacción de negocio (`RecordatorioService`/`InstanciaComunService` publican un evento; `CalendarSyncListener` lo procesa post-commit):
- Al crear/editar un `Recordatorio` (`POST`/`PUT /recordatorios`), se crea/actualiza el evento invitando al **responsable** (agenda interna, sin el alumno).
- Al crear/editar una instancia (`POST`/`PUT /instancias`), se crea/actualiza el evento invitando al **funcionario y al alumno**.
- El evento usa el título real de la instancia/recordatorio y una descripción con categoría + tipo/canal + ID de negocio (`INST-2026-...`/`REC-2026-...`) — el mismo ID también va al final del título, para poder ubicar el caso en el sistema desde la invitación o desde un listado de Calendar.
- **Tolerancia a fallos:** si los servidores de Google se caen, la API no lanza error — el recordatorio/instancia se guarda igual en la base de datos local, y se deja un registro de advertencia (`[WARN]`) en la consola del backend.

## Documentación de la API (Swagger)

Una vez que hayas levantado la base de datos y ejecutado la aplicación, la API estará documentada de forma dinámica e interactiva.

Para ver los modelos de datos, probar los endpoints y revisar los códigos de estado, ingresa desde tu navegador a (disponible mientras `SWAGGER_ENABLED` no sea `false`):

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**