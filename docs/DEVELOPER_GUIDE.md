# LibConnect Developer Guide

This guide describes the architecture, development workflow, persistence model,
testing strategy, and release process for LibConnect.

## Project conventions

- Target Java SE 25 and keep the Maven compiler release at `25`.
- Use the layered architecture described below.
- Keep business rules in services, persistence details in repositories, and UI
  behavior in pages/components or controllers.
- Depend on interfaces when crossing role boundaries.
- Persist stable IDs instead of nested copies of related entities.
- Add Javadoc to every new public class and public method.
- Use injected clocks and repository interfaces when a rule needs deterministic
  tests.

The repository contains project-specific guidance in `AGENTS.md` and the original
feature and architecture plan in `PLAN.md`.

## Architecture

```text
JavaFX pages/components
        |
        v
controllers and integration contracts
        |
        v
services and domain models
        |
        v
repository interfaces
        |
        v
file-backed repositories -> StorageManager -> JSON data files
```

### Main packages

| Package | Responsibility |
| --- | --- |
| `libconnect.models` | Immutable domain state, value validation, and state transitions. |
| `libconnect.services` | Authentication, catalogue, borrowing, reservation, fine, notification, and user rules. |
| `libconnect.integration` | Stable cross-role contracts such as `LoanQuery`, `MemberDirectory`, and `BookCatalogue`. |
| `libconnect.storage.repositories` | Persistence interfaces independent of the file format. |
| `libconnect.storage.file` | JSON-backed repository implementations. |
| `libconnect.storage` | Shared JSON parsing, safe replacement, and malformed-record recovery. |
| `libconnect.librarian` | Librarian authorization boundary and view-facing actions. |
| `libconnect.ui` | Composition roots, session state, navigation, and application startup. |
| `libconnect.ui.pages` | Feature pages and user interaction handlers. |
| `libconnect.ui.components` | Reusable JavaFX controls and presentation components. |

`LibConnectApplication` creates the runtime graph. `LibrarianCompositionRoot`
connects file repositories, services, integration providers, the controller, and
the JavaFX view. Services do not construct views and views do not read JSON files.

## Persistence

Each entity type has a JSON array file:

| File | Entity | Stable identifier |
| --- | --- | --- |
| `data/members.json` | `Member` | membership ID |
| `data/librarians.json` | `Librarian` | employee ID |
| `data/books.json` | `Book` | ISBN |
| `data/book-copies.json` | `BookCopy` | copy ID |
| `data/loans.json` | `Loan` | loan ID |
| `data/reservations.json` | `Reservation` | reservation ID |
| `data/fines.json` | `Fine` | fine ID |
| `data/notifications.json` | `Notification` | notification ID |

`StorageManager` creates a missing file as `[]`, deserializes valid records, skips
malformed records, and appends recovery information to
`data/malformed/malformed-records.json`. Writes use a temporary file followed by a
replacement move. `AbstractFileRepository` reloads the file for each operation so
the repository does not retain a stale in-memory snapshot.

When an operation updates multiple repositories, the service must not report
success after a partial write. Add rollback or journal support before introducing
another coordinated multi-file mutation.

## Cross-role integration

Member-owned and librarian-owned code communicate through stable interfaces:

- `MemberDirectory` answers whether a member exists and is active.
- `BookCatalogue` answers whether a book exists and is available.
- `LoanQuery` exposes the minimal loan summaries required for fine calculation and
  overdue notifications.
- `MemberManagement`, `BookManagement`, and `BookCopyManagement` expose librarian
  actions without exposing member-role repositories.
- `FineIssuer` lets borrowing return logic issue a fine without depending on the
  fine repository.

Add a new method to a contract only when the use case cannot be expressed through
the existing stable ID-based operations. Add a contract test for valid, missing,
duplicate, and invalid-ID cases when the contract changes.

## Testing strategy

Tests are grouped by the behavior they verify:

- `src/test/java/libconnect/models`: model invariants and state transitions.
- `src/test/java/libconnect/services/unit`: service rules using in-memory doubles.
- `src/test/java/libconnect/services/integration`: service flows crossing role and
  repository boundaries.
- `src/test/java/libconnect/storage`: storage safety and malformed-record recovery.
- `src/test/java/libconnect/storage/file/unit`: repository CRUD, query, validation,
  round-trip, and malformed-data behavior.
- `src/test/java/libconnect/ui`: page, navigation, and JavaFX interaction behavior.

For a new or modified class:

1. Search for its existing test file before writing code.
2. Add focused tests for valid input, boundary values, missing records, invalid
   input, and persistence or integration effects where applicable.
3. Add an integration test when the change crosses a repository, service, role, or
   UI boundary.
4. Run the full suite before handing off the change.

Run the test suite:

```text
mvn test
```

Expected result:

```text
Tests run: <number>, Failures: 0, Errors: 0, Skipped: <number>
BUILD SUCCESS
```

Generate the coverage report:

```text
mvn clean verify
```

Expected result:

```text
BUILD SUCCESS
target/site/jacoco/index.html is available for review.
```

The JaCoCo report is diagnostic: it makes uncovered branches visible without
turning the existing UI-heavy suite into an arbitrary percentage gate. Coverage
work should prioritize domain rules, persistence failure paths, authorization, and
cross-role flows before presentation-only getters or JavaFX layout code.

## Adding a feature

1. Identify the owning role and read the matching section of `PLAN.md`.
2. Reuse an existing model, validator, clock, ID generator, repository method, or
   integration contract when one already expresses the requirement.
3. Add or generalize the repository interface before adding the file implementation.
4. Put validation and business invariants in the service boundary.
5. Wire the service through the relevant composition root.
6. Keep the controller thin and keep persistence out of views.
7. Add unit tests, then integration/UI tests for the boundary crossed.
8. Run `mvn clean verify` and inspect the coverage report.

## Release process

The project version is `1.0.0`. The release profile creates a platform-specific,
self-contained executable JAR containing:

- `libconnect-1.0.0.jar` with the launcher manifest;
- JavaFX and Jackson runtime dependencies shaded into the JAR.

The application creates missing JSON data files in the working directory. Keep the
`data/` directory beside the JAR when distributing pre-populated records, and run
the JAR from a directory where the application can write its data files.

Build it with:

```text
mvn -Prelease clean package
```

Expected result:

```text
BUILD SUCCESS
target/libconnect-1.0.0.jar is ready for distribution.
```

The release profile also stages the same artifact at
`release/libconnect-1.0.0.jar` for local acceptance testing.

The JavaFX classifier is selected from the host operating system. Build the JAR on
the target platform before distributing it, because JavaFX native libraries are
platform-specific.

Run the release JAR on macOS or Linux:

```text
java -jar libconnect-1.0.0.jar
```

Expected result:

```text
The LibConnect login window opens.
```

## CI and troubleshooting

CI uses Java 25 and runs the full Maven verification lifecycle on pushes and pull
requests. A local environment must also use Java 25; Java 17 is not a supported
fallback for this project.

Check the installed Java version:

```text
java --version
```

Expected output begins with:

```text
openjdk 25
```

If JavaFX tests fail before assertions run, confirm that the platform profile has
selected the correct JavaFX classifier and that the test environment provides a
desktop-compatible display. If a storage test leaves malformed data behind, use a
temporary test directory rather than the repository `data/` directory and inspect
the recovery journal created by the test.
