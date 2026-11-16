# Dependency upgrade - 2026-11-16

## Backend

- Java 27 (local Temurin 27.0.1.0.2), Gradle 9.9.1, Spring Boot 4.1.2.
- Jackson 3 application code, Spring Security 7, updated Boot test modules.
- springdoc 3.2.0 and OpenAPI Gradle plugin 2.1.0.
- JJWT 0.13.0 with the typed signing/parsing API; existing HS256 tokens remain supported.
- Published Brevo 1.1.11 replaces the local 1.0.0 JAR.
- OkHttp 5.5.1, Gson 2.15.0, GsonFire 1.9.0, ThreeTenBP 1.7.5,
  Commons Lang 3.22.0, Google API Client 2.9.2, OAuth Client 1.39.0.
- Google Auth 1.57.0: 1.58.0 currently references unavailable api-common 2.74.0.
- Guava 33.8.0 replaces Google's vulnerable transitive 33.6.0 (GHSA-xxph-c9ww-hj94).
- H2 2.5.252; remaining framework libraries and PostgreSQL JDBC use Boot's BOM.
- Docker uses the Java 27 JRE. The external PostgreSQL server is not upgraded by this change.

## Local setup

JAVA_HOME now points to C:/Users/linus/Java/jdk-27.0.1.0.2+1.
Flutter stable is installed separately at C:/Users/linus/Flutter/flutter-stable-current.
Existing Flutter SDK modifications were preserved. Restart terminals/editors to reload environment variables.

## Frontend

See the frontend repository's DEPENDENCY_UPGRADE.md for package versions,
storage compatibility, native build requirements and verification results.

## Verification

Backend integration/unit tests, executable JAR and generated OpenAPI must pass together.
Verified: all 167 tests and the full Gradle build including OpenAPI pass on Java 27.
Real PostgreSQL schema upgrades and external Google/Brevo/FCM delivery require deployment smoke tests.
The existing tests use H2 and mocked external services; they do not prove live service delivery.
