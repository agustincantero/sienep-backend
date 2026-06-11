# PDI Tercer Semestre DZA Grupo 1 – SIENEP

**Sistema Integral de Estudiantes con Necesidades Educativas Personalizadas**

Este repositorio contiene el backend (API REST) de **SIENEP**, desarrollado en **Java (Spring Boot)** con **Apache Maven**, que utiliza **PostgreSQL** para la persistencia de datos.

Forma parte del **Proyecto de Desarrollo e Infraestructura** del tercer semestre de la **Licenciatura en Tecnologías de la Información** en **UTEC**.

**SIENEP** centraliza y asegura la gestión educativa de **UTEC**, permitiendo al equipo institucional administrar la información de usuarios (funcionarios y estudiantes), realizar un seguimiento exhaustivo mediante instancias y recordatorios, gestionar incidencias y exportar reportes detallados, todo respaldado por un robusto sistema de roles y permisos.

## Tabla de contenidos

- [Requisitos previos](#requisitos-previos)
- [Funcionalidad principal](#funcionalidad-principal)
- [Arquitectura general](#arquitectura-general)
- [Configuración y Ejecución](#configuración-y-ejecución)
  - [Preparar la base de datos (Docker)](#preparar-la-base-de-datos-docker)
  - [Ejecución desde la Terminal](#ejecución-desde-la-terminal)
  - [Ejecución desde IntelliJ IDEA](#ejecución-desde-intellij-idea)
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
3. **Gestión de Instancias:** Registro, categorización, clonación, visualización e integración con Google Calendar.
4. **Gestión de Recordatorios:** Creación (únicos y recurrentes), notificaciones, vinculación a instancias e integración con Google Calendar.
5. **Gestión de Incidencias:** Registro detallado y visualización del historial de incidencias.
6. **Reportes:** Generación y exportación de informes del sistema.
7. **Administración y Configuración:** Gestión completa de Roles y Permisos (preexistentes y personalizados), y mantenimiento de las categorías de Instancias y Recordatorios.

## Arquitectura general

Este proyecto sigue una arquitectura en capas estándar de Spring Boot orientada al diseño de APIs RESTful:

- **Controladores (`controller/`):** Exponen los endpoints HTTP, documentados con Swagger (OpenAPI 3), y gestionan las peticiones y respuestas.
- **Servicios (`service/`):** Contienen la lógica de negocio central.
- **Repositorios (`repository/`):** Interfaces de Spring Data JPA para el acceso a PostgreSQL.
- **Modelo (`model/`):** Entidades JPA mapeadas directamente al esquema relacional.
- **DTOs (`dto/`):** Objetos de transferencia de datos. Se utiliza el patrón CQRS simplificado separando las entradas de las salidas para mantener validaciones limpias e independientes.

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

*Windows - Opción 1: PowerShell*
```powershell
$env:JWT_SECRET="SienepGrupo01DZA2026"
$env:DB_URL="jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto"
$env:DB_USER="sienep"
$env:DB_PASSWORD="sienep"
```

*Windows - Opción 2: Símbolo del sistema (CMD)*
```cmd
set JWT_SECRET=SienepGrupo01DZA2026
set DB_URL=jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto
set DB_USER=sienep
set DB_PASSWORD=sienep
```

*macOS & Linux (bash/zsh)*
```bash
export JWT_SECRET="SienepGrupo01DZA2026"
export DB_URL="jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto"
export DB_USER="sienep"
export DB_PASSWORD="sienep"
```

**Paso 2: Ejecutar el proyecto**

Una vez configuradas las variables en tu terminal, ejecutá el proyecto usando Maven Wrapper:

*   **En Windows:** `.\mvnw spring-boot:run`
*   **En macOS/Linux:** `./mvnw spring-boot:run`

### Ejecución desde IntelliJ IDEA

1. Abrí el proyecto en IntelliJ.
2. Entrá en **Run → Edit Configurations…**.
3. Hacé clic en **Add New Configuration → Application**.
4. En el campo **Name**, escribí por ejemplo `RunSienep`.
5. Completá los campos:
   * **Main class**: la clase principal de la aplicación (`com.fides.sienep.SienepApplication`).
   * **Working directory**: ruta del proyecto (se completa automáticamente).
6. En **Environment variables**, agregá lo siguiente:
   ```text
   JWT_SECRET=SienepGrupo01DZA2026;DB_URL=jdbc:postgresql://localhost:5432/sienep?currentSchema=proyecto;DB_USER=sienep;DB_PASSWORD=sienep
   ```
   *(También podés usar el ícono de carpeta al lado del campo para cargarlas en forma de tabla).*
7. Aplicá los cambios (**Apply → OK**).
8. Ejecutá la aplicación con el botón ▶️.

## Documentación de la API (Swagger)

Una vez que hayas levantado la base de datos y ejecutado la aplicación, la API estará documentada de forma dinámica e interactiva.

Para ver los modelos de datos, probar los endpoints y revisar los códigos de estado, ingresa desde tu navegador a:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**
