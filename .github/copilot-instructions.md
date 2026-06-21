# Copilot instructions for SIENEP

## Project overview
SIENEP is a REST API backend for UTEC built with Java 21, Spring Boot 4.0.6, Spring WebMVC, Spring Data JPA, Spring Security, JWT, MapStruct, Swagger/OpenAPI, and PostgreSQL 16+.

## Main architecture
- Layered structure: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `security`, `config`, `exception`, `utils`.
- DTOs are mandatory and should decouple the API from the persistence model.
- Use CQRS-style DTO pairs: `REQUEST`/`UPDATE` inputs and `RESPONSE` outputs.
- The `Usuario` hierarchy uses `InheritanceType.JOINED` for `Estudiante` and `Funcionario`.
- All database objects live in the `proyecto` schema.

## Persistence and domain rules
- `spring.jpa.hibernate.ddl-auto` must remain `validate`.
- Use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` for enum-backed columns.
- Initialize default entity values directly in the field declaration.
- Composite keys for join tables with payload must use a static inner `@Embeddable` class inside the entity.
- Never use physical deletes; logical deletion must be done via state changes such as `INACTIVO` or `ELIMINADO`.

## Business validation rules
- Validate Uruguayan CI numbers with the verifier-digit algorithm before saving.
- Enforce the minimum age rule (18+) where applicable.
- Do not expose credentials in responses, logs, queries, or error messages.
- Follow the database/schema-specific password storage constraint already used by the project when touching credential code; preserve the project's existing credential encoding behavior and never expose plaintext values.

## Security and access control
- Authentication is JWT-based and stateless.
- Use `@PreAuthorize("hasAuthority('PERMISO')")`; never use `hasRole(...)`.
- Controllers and services should preserve the existing authority model for custom roles.
- The app uses Spring Security plus the existing JWT filter and user-details services.

## Auditing
- Create, update, delete, login, and logout actions must be audited.
- Audit records must include user, date/time, action, and affected entity.
- When injecting session variables with `JdbcTemplate`, validate HTTP input with a strict regex such as `^[a-zA-Z0-9.:]+$` before concatenation.

## API conventions
- Controllers must map under `/api/<recurso-en-plural>` only; no version prefixes.
- Use `@PathVariable` only for identifying one resource.
- Use `@RequestParam` only for filtering or searching.
- Every controller method must include at least `@Operation` and `@ApiResponse`.

## Service and error-handling conventions
- Use `@RequiredArgsConstructor` with `private final` dependencies; do not use manual `@Autowired` constructors.
- Use `ResponseStatusException` for HTTP errors; do not throw generic `RuntimeException`.
- Avoid silent `catch` blocks; log captured exceptions with `@Slf4j` or document the explicit reason.
- Preserve the existing aggregate-root pattern: join tables and intermediate relationships do not get their own controllers or services.

## Code style used in this repo
- Prefer imperative loops (`for`, `while`) over streams, threads, or complex functional chains.
- Reuse existing mappers, validators, and DTO naming patterns.
- Keep sensitive data out of responses, logs, and exceptions.

## Functional coverage to preserve
- M1: RF01 login screen, RF02 credential authentication, RF03 credential management, RF04 logout.
- M2: RF05 student creation, RF06 logical deletion, RF07 edit, RF08 search, RF09 medical report attachment.
- M3: RF10 instance registration, RF11 categorization, RF12 visualization, RF13 Google Calendar integration, RF14 instance ID generation, RF15 instance ID notification, RF16 instance management, RF17 cloning, RF18 new instance from student record.
- M4: RF19 reminder creation, RF20 recurring reminders, RF21 reminder type assignment, RF22 Google Calendar integration, RF23 reminder notifications, RF24 reminder ID generation, RF25 reminder management, RF26 reminder visualization, RF27 create instance from reminder.
- M5: RF28 incident registration, RF29 incident history.
- M6: RF30 report generation, RF31 report export.
- M7: RF32 role management, RF33 predefined roles, RF34 custom roles, RF35 add instance categories, RF36 remove instance categories, RF37 modify instance categories, RF38 add reminder categories, RF39 remove reminder categories, RF40 modify reminder categories.

## Build, test, and run
- Start the database: `docker compose up -d`
- Build: `./mvnw clean install`
- Run the app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run one test class: `./mvnw -Dtest=RolServiceTest test`
- Run one test method: `./mvnw -Dtest=RolServiceTest#obtenerPorId_DebeRetornarDTO_CuandoElRolExiste test`

## Git and delivery standards
- Use feature or fix branches with descriptive names.
- In IntelliJ, name branches like `feature/lo que están haciendo y la clase` or `fix/lo que corrigieron y la clase`.
- `main` is for stable/integrated code only.
- Commit messages must follow `Type: Description` (for example, `Feature: Add user login`), not conventional commit scopes.
- Do not use generic commit messages like `update` or `cambios varios`, and do not mention team members by name.
- Use merge requests before merging into `main`.
