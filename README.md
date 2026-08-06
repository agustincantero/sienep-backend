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
    - [Recuperación de contraseña](#recuperación-de-contraseña)
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

1. **Autenticación y Acceso:** Inicio y cierre de sesión, y gestión segura de credenciales.
2. **Gestión de Estudiantes:** Altas, bajas lógicas, modificaciones, búsquedas y capacidad para adjuntar informes médicos.
3. **Gestión de Instancias:** Registro, categorización, visualización e integración con Google Calendar (creando un recordatorio inicial en la misma llamada, ver RF13). La clonación (RF17) se resuelve con los endpoints estándar de lectura y creación: el Frontend trae los datos de la instancia original con `GET /api/instancias/{id}`, precarga un formulario editable (fecha, canal, categoría, etc.) y crea la nueva instancia con `POST /api/instancias`. No existe un endpoint dedicado a "clonar" porque no tendría sentido una copia idéntica sin edición previa por parte del usuario.
4. **Gestión de Recordatorios:** Creación (únicos y recurrentes), notificaciones, vinculación a instancias e integración con Google Calendar.
5. **Gestión de Incidencias:** Registro detallado y visualización del historial de incidencias.
6. **Reportes:** Generación y exportación de informes del sistema, incluyendo un endpoint público (`POST /api/reportes/validar`) para verificar si un PDF exportado es auténtico, comparando su hash SHA-256 contra el que el sistema guardó al generarlo.
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
- **Facade (Fachada) - `GoogleOAuthService`:** En lugar de exponer la complejidad del SDK oficial de Google (`NetHttpTransport`, `GsonFactory`, y múltiples requests) hacia las capas superiores, esta clase encapsula todo este subsistema engorroso. Lo oculta detrás de métodos de negocio extremadamente limpios, liberando completamente al `AuthController` de conocer los detalles de la infraestructura externa.
- **Builder (Constructor):** Patrón creacional implementado a través de la anotación `@Builder` de Lombok, especialmente en la capa de DTOs y en conjunto con `MapStruct`. Se eligió este patrón para evitar la proliferación de constructores gigantes y asegurar la inmutabilidad en el mapeo de objetos, permitiendo ensamblar instancias complejas paso a paso de forma legible.
- **Singleton (Instancia Única):** Patrón creacional adoptado en toda la capa de lógica y acceso a datos (`@Service`, `@Repository`, `@RestController`). En lugar de instanciar manualmente objetos pesados (que agotarían la memoria), el equipo delegó la gestión del patrón Singleton al Contenedor de Inversión de Control (IoC) de Spring Boot. Spring garantiza que exista una única instancia global de estos componentes en memoria, inyectándolos dinámicamente donde se necesiten.


## Arquitectura de Seguridad y Autenticación

El sistema implementa una arquitectura de seguridad robusta y altamente personalizada basada en **Spring Security** y **JWT** (JSON Web Tokens), operando de forma 100% *stateless* (sin almacenamiento de sesiones en el servidor).

A continuación se detalla la responsabilidad de las clases clave que componen este flujo, un aspecto fundamental del diseño del sistema:

*   **`SecurityConfig`:** Configura el comportamiento central de Spring Security. Deshabilita CSRF (apropiado para APIs REST con JWT), establece la política de sesión sin estado (`STATELESS`), define qué endpoints son públicos (como `/api/auth/**` y Swagger) y exige autenticación para el resto. También registra el filtro JWT para que actúe antes de los filtros estándar.
*   **`AuthController` y `AuthService`:** Exponen y procesan el endpoint `/api/auth/login`. Gestionan la validación inicial, la protección contra ataques de fuerza bruta (bloqueando cuentas tras 5 intentos fallidos en 10 minutos) y auditan el acceso exitoso o denegado. Si las credenciales son válidas, solicitan la emisión del Token.
*   **`LoginUsuarioDetailsService`:** Implementación de `UserDetailsService`. Su única función es asistir en el proceso de **Autenticación (Login)**. Busca al usuario en la base de datos (sea `Funcionario` o `Estudiante`) utilizando su **correo electrónico**. Esto permite a Spring validar la contraseña provista contra el hash encriptado de la base de datos.
*   **`JwtTokenUtil`:** Clase utilitaria encargada de la criptografía. Al generar el Token, asigna el `idUsuario` (la clave primaria) como el *Subject* (sujeto) en lugar del email, mejorando la privacidad y la eficiencia de consultas futuras. Los tokens tienen una validez configurada (ej. 5 horas) y están firmados con algoritmos seguros.
*   **`JwtRequestFilter`:** Filtro interceptor que evalúa cada petición entrante HTTP. Busca el header `Authorization: Bearer <token>`, extrae el JWT, verifica su validez y expiración, y obtiene el `idUsuario` embebido. Si es válido, inyecta la identidad autenticada en el `SecurityContextHolder`.
*   **`CustomUsuarioDetailsService`:** Utilizado exclusivamente por el `JwtRequestFilter` para la fase de **Autorización**. A diferencia del servicio de Login, esta clase busca al usuario directamente por su **ID** y levanta sus privilegios en memoria:
    *   **Funcionarios:** Convierte el Rol y la lista de permisos (`RolPermiso`) asignados dinámicamente en la base de datos a instancias de `GrantedAuthority` de Spring.
    *   **Estudiantes:** Se les inyecta un rol sintético (`ROLE_ESTUDIANTE`) y permisos base (`VER_COMENTARIO_NORMAL_ESTUDIANTE`), logrando que ambos actores convivan transparentemente bajo el mismo control de acceso basado en anotaciones (`@PreAuthorize`).

### Recuperación de contraseña

Si el usuario olvida su contraseña, el flujo se separa en dos pasos para no aplicar el cambio sin confirmación:

1. **`POST /api/auth/olvide-contrasenia`** (público): recibe un email, genera un token de un solo uso (UUID), guarda su hash SHA-256 con vencimiento de 30 minutos e invalida cualquier token anterior del mismo usuario. Envía por correo un link armado con la variable de entorno `FRONTEND_URL` (ej. `https://tu-frontend/restablecer-contrasenia?token=...`). La contraseña actual no se modifica todavía en este paso.
2. **`POST /api/auth/restablecer-contrasenia`** (público): recibe el token y la nueva contraseña, valida que no esté vencido ni ya usado, y recién ahí la aplica.

Por diseño, este flujo responde siempre igual (200) exista o no el email, para no permitir enumerar cuentas registradas, y limita a **3 solicitudes cada 15 minutos por email** (`429 Too Many Requests` si se supera), para frenar abuso hacia el correo del usuario o hacia el servidor SMTP.

## Archivos subidos

El sistema permite adjuntar archivos reales (no URLs externas) en dos flujos:

- **Informes adjuntos de un estudiante:** `POST /api/informes-adjuntos` (multipart) sube el archivo; `GET /api/informes-adjuntos/{id}/descargar` lo devuelve, protegido por el permiso `VER_INFORME`.
- **Foto de perfil de un estudiante:** `PUT /api/estudiantes/{id}/foto` (multipart) sube/reemplaza la foto; `GET /api/estudiantes/{id}/foto` la devuelve. El propio estudiante puede subir y ver su foto sin necesitar el permiso general de edición/visualización.

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

> **Nota:** además de las variables obligatorias definidas abajo, existen 3 opcionales (si no se definen, usan su valor por defecto):
> - `FRONTEND_URL`: URL base del frontend, usada para armar el link de "olvidé mi contraseña" (default `http://localhost:3000`).
> - `STORAGE_PATH`: carpeta base donde se guardan los archivos subidos — informes adjuntos y fotos de perfil (default `uploads`).
> - `CORS_ALLOWED_ORIGINS`: orígenes permitidos para CORS, separados por coma y sin barra final (default `http://localhost:3000,https://sienep.app`). El backend solo acepta peticiones `fetch`/AJAX hechas desde un origen de esta lista.

*Windows - Opción 1: PowerShell*
```powershell
$env:JWT_SECRET="SienepGrupo01DZA2026_ClaveSecretaMuySegura"
$env:DB_URL="jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto"
$env:DB_USER="sienep"
$env:DB_PASSWORD="sienep"
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="tu_correo@gmail.com"
$env:MAIL_PASSWORD="tu_password_de_aplicacion"
$env:GOOGLE_CLIENT_ID="tu_google_client_id"
$env:GOOGLE_CLIENT_SECRET="tu_google_client_secret"
$env:GOOGLE_REDIRECT_URI="http://localhost:8080/api/auth/google/callback"
$env:FRONTEND_URL="http://localhost:3000"
$env:STORAGE_PATH="uploads"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000,https://sienep.app"
```

*Windows - Opción 2: Símbolo del sistema (CMD)*
```cmd
set JWT_SECRET=SienepGrupo01DZA2026_ClaveSecretaMuySegura
set DB_URL=jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto
set DB_USER=sienep
set DB_PASSWORD=sienep
set MAIL_HOST=smtp.gmail.com
set MAIL_PORT=587
set MAIL_USERNAME=tu_correo@gmail.com
set MAIL_PASSWORD=tu_password_de_aplicacion
set GOOGLE_CLIENT_ID=tu_google_client_id
set GOOGLE_CLIENT_SECRET=tu_google_client_secret
set GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback
set FRONTEND_URL=http://localhost:3000
set STORAGE_PATH=uploads
set CORS_ALLOWED_ORIGINS=http://localhost:3000,https://sienep.app
```

*macOS & Linux (bash/zsh)*
```bash
export JWT_SECRET="SienepGrupo01DZA2026_ClaveSecretaMuySegura"
export DB_URL="jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto"
export DB_USER="sienep"
export DB_PASSWORD="sienep"
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="tu_correo@gmail.com"
export MAIL_PASSWORD="tu_password_de_aplicacion"
export GOOGLE_CLIENT_ID="tu_google_client_id"
export GOOGLE_CLIENT_SECRET="tu_google_client_secret"
export GOOGLE_REDIRECT_URI="http://localhost:8080/api/auth/google/callback"
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
   JWT_SECRET=SienepGrupo01DZA2026_ClaveSecretaMuySegura;DB_URL=jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto;DB_USER=sienep;DB_PASSWORD=sienep;MAIL_HOST=smtp.gmail.com;MAIL_PORT=587;MAIL_USERNAME=tu_correo@gmail.com;MAIL_PASSWORD=tu_password_de_aplicacion;GOOGLE_CLIENT_ID=tu_google_client_id;GOOGLE_CLIENT_SECRET=tu_google_client_secret;GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback;FRONTEND_URL=http://localhost:3000;STORAGE_PATH=uploads;CORS_ALLOWED_ORIGINS=http://localhost:3000,https://sienep.app
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

> **Aclaración Arquitectónica:** Por decisión del equipo, el esfuerzo de *Unit Testing* se centró exclusivamente en la **capa de Servicios** (`service/`), ya que es donde reside el núcleo de la lógica de negocio, reglas de validación y orquestación transaccional. Por este motivo, al leer el reporte se recomienda enfocar el análisis en dicho paquete.

Para visualizar este reporte interactivo y comprobar la alta cobertura de la lógica de negocio, abrí en tu navegador web el siguiente archivo generado localmente:
👉 `target/site/jacoco/index.html`

## Integración con Google Calendar

El sistema permite que los funcionarios sincronicen sus cuentas para ver los recordatorios directamente en Google Calendar.

### Cómo probar la vinculación vía API (Postman)
Para vincular tu cuenta localmente sin necesidad de una interfaz gráfica, seguí estos pasos:
1. Asegurate de tener la variable `GOOGLE_REDIRECT_URI` configurada como `http://localhost:8080/api/auth/google/callback` y que esa URL esté autorizada en Google Cloud Console.
2. Iniciá sesión (`POST /api/auth/login`) con un usuario que tenga el permiso `GCAL_SYNC_RECORDATORIOS` y copiá el Token JWT devuelto.
3. En Postman, realizá una petición `GET` a `http://localhost:8080/api/auth/google/init` enviando el token en la pestaña Authorization (`Bearer Token`). **Importante:** En la configuración de Postman, desactivá la opción "Automatically follow redirects".
4. Al enviar la petición, el backend devolverá un `302 Found`. Copiá la URL gigante que viene en el header `Location` de la respuesta.
5. Pegá esa URL en tu navegador web habitual, iniciá sesión con tu cuenta de Google y otorgá los permisos de Calendar.
6. Google te devolverá a la API, la cual procesará y guardará tu token correctamente.

### Sincronización Automática
Una vez vinculado el calendario, la creación de eventos funciona sola:
- Al crear un `Recordatorio` (`POST /api/recordatorios`), si le asignás como `idResponsable` el ID de tu usuario, el evento aparecerá mágicamente en tu Google Calendar.
- **Tolerancia a fallos:** Si los servidores de Google se caen, la API no lanza error. El recordatorio se guarda igual en la base de datos local y se deja un registro de advertencia (`[WARN]`) en la consola del backend.

## Documentación de la API (Swagger)

Una vez que hayas levantado la base de datos y ejecutado la aplicación, la API estará documentada de forma dinámica e interactiva.

Para ver los modelos de datos, probar los endpoints y revisar los códigos de estado, ingresa desde tu navegador a:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**