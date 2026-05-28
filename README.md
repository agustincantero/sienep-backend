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
2. Ejecuta el siguiente comando:
   ```bash
   docker compose up -d
   ```

**Comandos útiles:**
* `docker compose ps` - Verifica que `sienep-db` esté corriendo.
* `docker compose stop` - Detiene la BD sin borrar datos.
* `docker compose down -v` - Detiene la BD y **borra los datos físicos** (útil si necesitas que el script SQL se vuelva a cargar desde cero).

Las credenciales y la URL JDBC ya están preconfiguradas en el archivo `application.properties` para conectarse a este contenedor.

### Ejecución desde la Terminal

El proyecto utiliza Maven Wrapper (`mvnw`), por lo que no necesitas tener Maven instalado globalmente.

* **Windows:** `.\mvnw spring-boot:run`
* **Linux/Mac:** `./mvnw spring-boot:run`

### Ejecución desde IntelliJ IDEA

1. Abre el proyecto en IntelliJ.
2. Espera a que termine de descargar e indexar las dependencias de Maven.
3. Busca la clase principal `SienepApplication.java` en `src/main/java/com/fides/sienep/`.
4. Haz clic en el ícono verde de "Play" (Run) en el margen izquierdo.

## Documentación de la API (Swagger)

Una vez que hayas levantado la base de datos y ejecutado la aplicación, la API estará documentada de forma dinámica e interactiva.

Para ver los modelos de datos, probar los endpoints y revisar los códigos de estado, ingresa desde tu navegador a:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**
