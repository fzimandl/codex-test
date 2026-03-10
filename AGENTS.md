# Repository Guidelines

## Project Structure & Module Organization
A standard Spring Boot layout keeps navigation simple: production code lives under `src/main/java/<package>/` grouped by bounded context (e.g., `user/`, `billing/`). Configuration, templates, and SQL scripts belong in `src/main/resources/` with profile-aware subfolders when necessary. Tests mirror the package tree in `src/test/java/`; integration fixtures or data builders sit in `src/test/resources/`. Shared architectural notes, API contracts, and ADRs belong in `docs/`, while helper scripts (database seeding, migrations) stay under `scripts/`.

## Build, Test, and Development Commands
Use the Maven wrapper so contributors avoid local version drift:
```
./mvnw clean verify     # compile, run unit + integration tests, and package the jar
./mvnw spring-boot:run  # start the application with dev profile
./mvnw test -DskipITs   # run only fast unit tests when iterating on logic
./mvnw spotless:apply   # apply formatting and import ordering
```
If you prefer Gradle, add equivalent `./gradlew` tasks but keep the Maven wrapper authoritative until the switch is documented.

## Coding Style & Naming Conventions
Follow the default Spring style: four-space indentation, 120-char soft wrap, and package-private visibility unless a type is shared outside its module. Classes use `PascalCase`, methods and variables use `camelCase`, and configuration keys use `kebab-case` (`my.feature.flag`). Keep controller names aligned with resources (`UserController`, `InvoiceController`). Static analysis is enforced through `spotless`, `checkstyle`, and `errorprone`; configuration files live at the repo root.

## Testing Guidelines
Write unit tests with JUnit 5 and Mockito, naming files `<ClassName>Test`. Integration tests using MockMvc or Testcontainers append `IT` (`UserControllerIT`). Prefer descriptive `@DisplayName` strings and isolate database state via embedded containers. Achieve at least 80% line coverage and ensure `./mvnw verify` passes before opening a pull request. Add regression tests whenever you fix a defect; failing scenarios should reproduce the reported issue verbatim.

## Commit & Pull Request Guidelines
Use Conventional Commit prefixes (`feat:`, `fix:`, `chore:`) so release notes stay consumable. Each pull request must: explain the business context, list technical changes, reference related Jira or GitHub issues, and attach screenshots or cURL output for API changes. Include database migration IDs, config toggles, and rollout considerations in the description. Keep PRs under ~400 lines to simplify reviews and call out follow-up work explicitly.
