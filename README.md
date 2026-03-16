## Projektüberblick
Dieses Repository enthält das Backend für die Bierlisten-App. Aktueller Funktionsumfang: Registrierung/Login (inkl. E-Mail-Verifizierung), JWT-Auth mit Refresh Tokens, Google Login, User-Profileinstellungen sowie Gruppen mit Mitgliedschaften und gruppenbezogenen Strichzählern.

Technisch basiert das Projekt auf Spring Boot 3.5, Java 21, Gradle und Spring Data JPA. Die Datenbank-Konfiguration erfolgt per Umgebungsvariablen, E-Mails werden über den Brevo-API-Client versendet, und es gibt keine automatische API-Dokumentation oder Migrationen.

## Feature-Matrix (Ist-Stand)
| Bereich | Status | Evidenz (Datei/Klasse) | Notiz |
| --- | --- | --- | --- |
| Registrierung + Login (E-Mail/Passwort) | Implementiert | AuthController, AuthService, VerificationService, SecurityConfig | E-Mail-Verifizierung ist Pflicht vor Login |
| JWT Access/Refresh Tokens | Implementiert | JwtTokenProvider, RefreshTokenService, RefreshToken | Ein Refresh Token pro User (Rotation) |
| Google Login (ID-Token) | Implementiert | AuthController, AuthService | Erstellt User falls nicht vorhanden |
| Passwort Reset | Teilweise | AuthController.resetPassword/resetPasswordResend, VerificationService | Code wird gesendet, aber kein Endpoint zum Bestätigen/Setzen des neuen Passworts |
| Nutzerprofil lesen/ändern | Implementiert | UserController, UserService, UserDto | Updates nur bei gültigem lastUpdated |
| Passwort ändern (eingeloggt) | Implementiert | UserController.updatePassword, UserService.updatePassword | Kein Check des alten Passworts |
| Nutzer-Einstellungen | Implementiert | UserSettingsController, UserSettingsService, UserSettings | Theme + AutoSync |
| Gruppen & Mitgliederverwaltung | Implementiert | GroupController, GroupService, Group, GroupMember | Mitgliedschaften pro Gruppe/User |
| Einladungen zu Gruppen | Fehlt | - | - |
| Striche pro Gruppe/History | Teilweise | GroupController, GroupService, GroupMember | Gruppenbezogener Counter vorhanden, keine History |
| Rollen/Admin pro Gruppe | Implementiert | GroupRole, GroupMember, GroupAuthorizationService | `ADMIN` und `MEMBER` werden in Mitgliedschaften geprüft |
| OpenAPI/Swagger | Fehlt | - | Keine automatische API-Doku |

## Architektur und Module
| Package | Zweck | Zentrale Komponenten |
| --- | --- | --- |
| controller | REST Endpunkte | AuthController, UserController, UserSettingsController, GroupController, PingController, TestController |
| service | Geschäftslogik | AuthService, UserService, UserSettingsService, RefreshTokenService, VerificationService, EmailService, BrevoEmailService |
| repository | Persistenz (JPA) | UserRepository, UserSettingsRepository, RefreshTokenRepository, VerificationTokenRepository, GroupRepository, GroupMemberRepository |
| model | JPA Entities | User, UserSettings, RefreshToken, VerificationToken, Group, GroupMember |
| dto | Request/Response DTOs | RegisterDto, LoginDto, GoogleLoginDto, UserDto, UserPasswordDto, UserSettingsDto, CounterIncrementDto, CounterResponseDto |
| security | JWT + Auth Filter | JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService |
| config | Spring Konfiguration | SecurityConfig, WebConfig |
| exception | Fehlerbehandlung | GlobalExceptionHandler |

## Datenmodell
| Entity (Tabelle) | Wichtige Felder | Beziehungen |
| --- | --- | --- |
| User (users) | id, email (unique), username, passwordHash, emailVerified, googleUser, createdAt, lastUpdated | 1:1 zu UserSettings, RefreshToken, VerificationToken |
| UserSettings (user_settings) | user_id, theme, lastUpdated | 1:1 zu User |
| RefreshToken (refresh_tokens) | token (unique), user_id, expiryDate | 1:1 zu User |
| VerificationToken (verification_tokens) | code (6-stellig), user_id, expiryDate | 1:1 zu User |
| GroupMember (group_members) | group_id, user_id, role, joinedAt, strichCount | n:1 zu Group, n:1 zu User |

Hinweis: Es gibt keine Migrationen (Flyway/Liquibase). Das Schema wird via `spring.jpa.hibernate.ddl-auto=update` automatisch angepasst.

## Auth & Security
- JWT Access Token (HS256) mit Subject = User-ID und Claim `username`. Secret muss Base64-codiert sein (`app.jwt.secret`).
- Access Token Ablaufzeit: 900000 ms (15 Minuten). Refresh Token Ablaufzeit: `app.jwt.refresh-exp-ms` = 15552000000 ms (~180 Tage).
- Refresh Tokens werden in der DB gespeichert; pro User existiert nur ein aktives Token (Rotation durch Löschen des alten Tokens).
- E-Mail-Verifizierung ist Voraussetzung für Login mit Passwort. Google Login markiert E-Mail als verifiziert.
- Passwort-Hashing mit BCrypt.
- Security: stateless, CSRF deaktiviert, CORS erlaubt aktuell nur `http://localhost:8100`. Rollen: nur ROLE_USER.
- Public Endpoints: `/api/v1/auth/**`, `/api/v1/ping`. Alle anderen Endpoints erfordern `Authorization: Bearer <accessToken>`.

### Gruppenautorisierung
- `GroupController` und `GroupService` verwenden zentral `GroupAuthorizationService`.
- Nicht eingeloggt: HTTP 401 mit `{"error":"Nicht authentifiziert"}`.
- Gruppenendpunkte mit Mitgliedschaftspflicht liefern bei fehlender Gruppe oder fehlender Mitgliedschaft bewusst HTTP 404 mit `{"error":"Gruppe nicht gefunden"}`.
- Wart-Prüfungen verwenden `GroupRole.ADMIN` und liefern für normale Mitglieder HTTP 403 mit `{"error":"Wart-Rechte erforderlich"}`.
- `POST /api/v1/groups/{groupId}/roles/promote` ist idempotent und liefert immer den aktuellen Stand des Ziel-Mitglieds zurück; ist das Ziel bereits `ADMIN`, bleibt die Antwort dennoch HTTP 200.
- Schlägt eine Promotion fehl, weil `targetUserId` kein Mitglied der Gruppe ist, liefert der Endpoint HTTP 404 mit `{"error":"Gruppenmitglied nicht gefunden"}`.

## API
Basis-Pfad: `/api/v1`  
Content-Type: `application/json`  
Zeitformat für `Instant`: ISO-8601, z.B. `2026-01-25T12:34:56Z`

### Fehlerantworten
- Validierung: HTTP 400 mit Feld-zu-Fehler Map  
  Beispiel: `{"email":"muss eine gültige E-Mail sein"}`
- Standardfehler: JSON mit `error`  
  Beispiel: `{"error":"User nicht gefunden"}`

### Endpunkte (Übersicht)
| Methode | Pfad | Auth | Query | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /auth/register | nein | - | RegisterDto {email, username, password} | {message} |
| POST | /auth/verify | nein | - | {email, code} | {accessToken, refreshToken, userEmail} |
| POST | /auth/resendVerify | nein | - | {email} | {message} |
| POST | /auth/login | nein | - | LoginDto {email, password} | {accessToken, refreshToken, userEmail} |
| POST | /auth/google | nein | - | GoogleLoginDto {idToken} | {accessToken, refreshToken, userEmail} |
| POST | /auth/refresh | nein | - | {refreshToken} | {accessToken, refreshToken, userEmail} |
| POST | /auth/resetPassword | nein | - | {email} | {message} |
| POST | /auth/resetPasswordResend | nein | - | {email} | {message} |
| GET | /user | ja | - | - | UserDto |
| PUT | /user | ja | - | UserDto {email, username, lastUpdated} | UserDto |
| POST | /user/updatePassword | ja | - | UserPasswordDto {email, password, lastUpdated} | {message} |
| POST | /user/logout | ja | - | {refreshToken} | {message} |
| DELETE | /user/delete/account | ja | - | - | {message} |
| GET | /user/settings | ja | - | - | UserSettingsDto |
| PUT | /user/settings | ja | - | UserSettingsDto {theme, lastUpdated} | UserSettingsDto |
| POST | /user/settings/verifyPassword | ja | - | {password} | {valid} |
| POST | /groups/{groupId}/roles/promote | ja | - | PromoteGroupMemberDto {targetUserId} | GroupMemberDto |
| GET | /ping | nein | - | - | {status} |
| GET | /email | ja | - | - | "OK" (text/plain, Test) |

### Beispiele
#### POST /auth/register
Request:
```json
{
  "email": "max@example.com",
  "username": "Max",
  "password": "supergeheim123"
}
```
Response:
```json
{
  "message": "Verifizierungscode gesendet"
}
```

#### POST /auth/verify
Request:
```json
{
  "email": "max@example.com",
  "code": "123456"
}
```
Response:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "uuid-refresh",
  "userEmail": "max@example.com"
}
```

#### POST /auth/resendVerify
Request:
```json
{
  "email": "max@example.com"
}
```
Response:
```json
{
  "message": "Code erneut gesendet"
}
```

#### POST /auth/login
Request:
```json
{
  "email": "max@example.com",
  "password": "supergeheim123"
}
```
Response:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "uuid-refresh",
  "userEmail": "max@example.com"
}
```

#### POST /auth/google
Request:
```json
{
  "idToken": "google-id-token"
}
```
Response:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "uuid-refresh",
  "userEmail": "max@example.com"
}
```

#### POST /auth/refresh
Request:
```json
{
  "refreshToken": "uuid-refresh"
}
```
Response:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "uuid-refresh-neu",
  "userEmail": "max@example.com"
}
```

#### POST /auth/resetPassword
Request:
```json
{
  "email": "max@example.com"
}
```
Response:
```json
{
  "message": "Reset Code gesendet"
}
```

#### POST /auth/resetPasswordResend
Request:
```json
{
  "email": "max@example.com"
}
```
Response:
```json
{
  "message": "Code erneut gesendet"
}
```

#### GET /user
Response:
```json
{
  "email": "max@example.com",
  "username": "Max",
  "lastUpdated": "2026-01-25T12:34:56Z",
  "googleUser": false
}
```

#### PUT /user
Request:
```json
{
  "email": "max@example.com",
  "username": "Max Neu",
  "lastUpdated": "2026-01-25T12:34:56Z"
}
```
Response:
```json
{
  "email": "max@example.com",
  "username": "Max Neu",
  "lastUpdated": "2026-01-25T12:34:56Z",
  "googleUser": false
}
```

#### POST /user/updatePassword
Request:
```json
{
  "email": "max@example.com",
  "password": "neuesPasswort123",
  "lastUpdated": "2026-01-25T12:34:56Z"
}
```
Response:
```json
{
  "message": "Passwort erfolgreich geändert"
}
```

#### POST /user/logout
Request:
```json
{
  "refreshToken": "uuid-refresh"
}
```
Response:
```json
{
  "message": "Logout erfolgreich"
}
```

#### DELETE /user/delete/account
Response:
```json
{
  "message": "Konto erfolgreich gelöscht"
}
```

#### GET /user/settings
Response:
```json
{
  "theme": "dark",
  "lastUpdated": "2026-01-25T12:34:56Z"
}
```

#### PUT /user/settings
Request:
```json
{
  "theme": "light",
  "lastUpdated": "2026-01-25T12:34:56Z"
}
```
Response:
```json
{
  "theme": "light",
  "lastUpdated": "2026-01-25T12:34:56Z"
}
```

#### POST /user/settings/verifyPassword
Request:
```json
{
  "password": "supergeheim123"
}
```
Response:
```json
{
  "valid": true
}
```

#### GET /ping
Response:
```json
{
  "status": "success"
}
```

#### GET /email
Response: `OK` (text/plain)  
Hinweis: Test-Endpunkt sendet eine E-Mail an eine im Code hardcodierte Adresse (siehe `TestController`).

## Setup & Run

### Voraussetzungen
- Java 21
- Gradle (Wrapper vorhanden)
- Datenbank: PostgreSQL (empfohlen).
- Optional: Docker/Compose

### Lokal starten
1) Umgebungsvariablen setzen (siehe Tabelle unten)
2) Starten:
```
./gradlew bootRun
```

### Build & Run als Jar
```
./gradlew bootJar
java -jar build/libs/backend.jar
```

### Docker/Compose
Voraussetzung: `backend.jar` muss im Projekt-Root liegen (Dockerfile erwartet diese Datei).
```
./gradlew bootJar
docker compose up --build -d
```
Hinweis: `compose.yml` nutzt externe Netzwerke `web` und `db` und startet keine DB. PostgreSQL-Compose liegt laut `instructions.txt` in einem anderen Repo.

### Migrationen/Seed
- Keine Migrationen oder Seed-Daten vorhanden.
- Schema-Updates erfolgen automatisch via Hibernate (`ddl-auto=update`).

## Konfiguration
Alle Werte werden aus Umgebungsvariablen gelesen (siehe `application.properties` und `.env.example`).

| Key | Bedeutung |
| --- | --- |
| SPRING_DATASOURCE_URL | JDBC URL zur Datenbank |
| SPRING_DATASOURCE_USERNAME | DB Benutzer |
| SPRING_DATASOURCE_PASSWORD | DB Passwort |
| JWT_SECRET | Base64-Secret für JWT Signatur |
| MAIL_HOST | SMTP Host (aktuell nur für Spring Mail Config) |
| MAIL_USERNAME | SMTP Benutzer |
| MAIL_PASSWORD | SMTP Passwort |
| MAIL_API_KEY | Brevo API Key (wird aktiv genutzt) |
| GOOGLE_CLIENT_ID | Google OAuth Client ID (ID-Token Verifizierung) |

Weitere Properties:
- `app.jwt.access-exp-ms` (Access Token TTL)
- `app.jwt.refresh-exp-ms` (Refresh Token TTL)
- `app.verif-exp-minutes` (Ablaufzeit für Verifizierungscode)
- `server.port` (Standard 8080)
- CORS Originen in `WebConfig`

## Tests
```
./gradlew test
```

## Vollständigkeit (Definition)
| Sicht | "Vollständig" bedeutet |
| --- | --- |
| Produkt (MVP) | Nutzer können Gruppen erstellen, Mitglieder einladen, Striche pro Gruppe erfassen und ihre eigenen Daten verwalten. |
| Technik | Abgesicherte Auth-Flows, valide Datenmodelle mit Migrationen, nachvollziehbare Fehlerbehandlung, Tests für Kernflüsse, grundlegende Observability. |

## Roadmap (priorisiert)
| Prio | Schritt | Grund |
| --- | --- | --- |
| P0 | Gruppen, Mitglieder und Rollenmodell implementieren | Kernfeature der App fehlt |
| P0 | Striche pro Gruppe + Historie (Counter pro Gruppe) | Kernfeature der App fehlt |
| P0 | Passwort-Reset komplettieren (Code prüfen + neues Passwort setzen) | Flow aktuell unvollständig |
| P0 | Test-Email-Endpunkt entfernen | War nur für Tests |
| P1 | Migrations-Tool (Flyway/Liquibase) einführen | Sauberes Schema-Management |
| P1 | OpenAPI/Swagger Doku erzeugen | Wartbarkeit/Integration |
| P1 | Autorisierung auf Ressourcenebene (z.B. Gruppenrechte) | Sicherheit |
| P2 | Observability (strukturierte Logs, Metriken, Tracing) | Betrieb & Debugging |
| P2 | Mehr Tests (Service/Controller/Integration) | Qualität |
