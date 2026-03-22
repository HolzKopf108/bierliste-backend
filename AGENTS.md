# AGENTS.md

## Projekt
Dieses Repository enthält das Backend der Bierliste-App.
Tech-Stack:
- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Security
- Validation
- PostgreSQL
- H2 für Tests
- Gradle

## Ziel
Arbeite so, dass bestehende Architektur, Namenskonventionen und Patterns des Repos beibehalten werden.

## Wichtige Konventionen
- DTOs enden immer auf `Dto`
- Keine unnötigen Umbenennungen bestehender Klassen/Felder
- Bestehende Package-Struktur beibehalten
- Änderungen klein und fokussiert halten
- Keine großen Refactorings ohne ausdrückliche Aufforderung
- Bestehende Error-Handling-Patterns respektieren
- Validierung über Jakarta Validation nutzen
- Verständliche HTTP-Statuscodes und Fehlermeldungen liefern
- Bei neuen API-Endpunkten bestehende URL-Struktur und Stil beibehalten

## Backend-Richtlinien
- Business-Logik in Services, nicht in Controller
- Controller bleiben dünn
- Datenbankmodell mit JPA sauber und explizit modellieren
- Constraints nach Möglichkeit sowohl in JPA als auch DB-seitig berücksichtigen
- Für Create/Update-Flows passende DTOs verwenden
- Keine Entitys direkt ungefiltert als API-Response zurückgeben
- Für neue Persistenzlogik passende Repository-Methoden ergänzen
- Transaktionale Schreiboperationen mit `@Transactional`, wenn mehrere DB-Schritte atomar zusammengehören

## Tests
- Vorhandene Testpatterns im Repo beibehalten
- Für neue Features mindestens die relevanten Service- oder Integrationstests ergänzen
- H2 für DB-nahe Tests verwenden, wenn das im bestehenden Repo so genutzt wird
- Tests nicht unnötig kompliziert machen

## Sicherheit
- Geschützte Endpunkte müssen Auth berücksichtigen
- Vorhandene Security-Mechanismen nicht umgehen
- Keine sicherheitsrelevanten Änderungen ohne klaren Bedarf

## Stil
- Präzise, kleine Commits/Änderungen
- Bestehenden Code-Stil des Repos nachahmen
- Keine inline Kommentare
- Keine neuen Libraries einführen, wenn nicht nötig

## Bei Unklarheit
- Erst bestehende Implementierungen im Repo prüfen
- Ähnliche Controller/Services/Tests im Repo als Vorlage verwenden
- Lieber konsistent mit dem Projekt als generisch “schön”

## Projektspezifisch
- `GlobalExceptionHandler` ist maßgeblich für API-Fehlerformat
- Fehlerresponses sollen zum bestehenden Format passen:
  - Validation: `Map<String, String>`
  - Statusfehler: `{ "error": "..." }`
- Bestehende User- und Auth-Patterns zuerst prüfen und wiederverwenden
- Datenbankschema gehört primär in die Java-/JPA-Definitionen:
  - Tabellen, Spalten, Indizes, Defaults und Constraints nach Möglichkeit über Entity-Mapping und passende Hibernate-/JPA-Annotationen modellieren
  - `schema.sql` nicht für reguläre Schema-Erstellung oder laufende Schema-Pflege verwenden, sondern nur für idempotente Einmal-Migrationen bestehender Datenbestände, die beim ersten Start nach einem Update nötig sind
  - Solche Einträge in `schema.sql` müssen so formuliert sein, dass sie bei späteren Starts keine weiteren Änderungen mehr verursachen und als No-Op durchlaufen
  - Wenn für bestehende Datensätze keine solche Startmigration nötig ist, sollte `schema.sql` leer bleiben
- Bei neuen Features zuerst prüfen, wie ähnliche Features bereits im Repo umgesetzt sind
- Wenn ein GitHub-Issue umgesetzt wird:
  - Akzeptanzkriterien vollständig abarbeiten
  - nur Scope des Issues umsetzen
  - Folgefeatures nicht vorziehen
  
