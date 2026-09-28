# LibConnect Developer Guide

This guide is for developers who build, extend, test, troubleshoot, and release
LibConnect. It describes the current JavaFX application, not the unimplemented
items in `PLAN.md`.

## Start here

### Toolchain

- JDK 25, including a compiler (`javac`).
- Maven 3.9 or later.
- A desktop-capable display for the JavaFX application and JavaFX UI tests.

The `pom.xml` targets Java 25, JavaFX 25, JUnit 5.14.4, Jackson 2.22.2, and
JaCoCo 0.8.14. Verify both Java and Maven use JDK 25:

```text
java --version
javac --version
mvn --version
```

Expected result: Java and Maven report version 25 and `javac` is available. A
JRE-only Maven environment fails during compilation with `No compiler is
provided in this environment`.

### Repeatable commands

Run the test suite:

```text
mvn test
```

Run a clean build, tests, verification, and the JaCoCo report:

```text
mvn clean verify
```

Run the application from the checkout:

```text
mvn exec:java
```

The configured main class is `libconnect.ui.LibConnectLauncher`. The expected
successful result is a LibConnect JavaFX window, not a command-line prompt.

## Architecture and dependency direction

LibConnect uses a layered design:

```text
JavaFX pages and components
            |
            v
controllers and stable integration contracts
            |
            v
services and domain models
            |
            v
repository interfaces
            |
            v
JSON file repositories -> StorageManager -> data/*.json
```

The intended direction is from presentation toward services and abstractions.
Models do not read files, services depend on repository interfaces, and views do
not know JSON formats.

### Package responsibilities

| Package | Responsibility |
| --- | --- |
| `libconnect.models` | Domain entities, value validation, statuses, and state transitions. |
| `libconnect.services` | Authentication, users, catalogue, copies, borrowing, loans, reservations, fines, and notifications. |
| `libconnect.integration` | Stable ID-based contracts between member-owned and librarian-owned functionality. |
| `libconnect.storage.repositories` | Persistence abstractions and repository-specific contracts. |
| `libconnect.storage.file` | JSON-backed repository implementations. |
| `libconnect.storage` | Shared JSON mapping, file initialization, malformed-record recovery, and safe replacement. |
| `libconnect.librarian` | Librarian authorization controller and view boundary. |
| `libconnect.ui` | JavaFX startup, navigation, session state, runtime composition, and dependency wiring. |
| `libconnect.ui.pages` | Login, registration, member, and librarian screens. |
| `libconnect.ui.components` | Reusable controls such as cards, forms, navigation, feedback, and the librarian view adapter. |

### Composition roots

`LibConnectApplication.start` creates the shared member services, session, clock,
and `JavaFxLibrarianView`, then delegates librarian wiring to
`LibrarianCompositionRoot` and passes the resulting `LibrarianRuntime` to
`SceneNavigator`.

`LibrarianCompositionRoot` is the file-backed composition root. It creates a
`StorageManager`, the member/librarian/book/copy/loan repositories, the librarian
services, member-role adapters, `ReservationService`, `FineService`,
`NotificationService`, and `LibrarianController`. Its constructor parameters
allow tests to substitute a temporary data directory, a fixed `Clock`, and a
test view.

`SceneNavigator` guards page transitions with the current session. Member pages
are shown for member sessions; the librarian workspace is shown only when the
session contains an active `Librarian`.

## Domain and service boundaries

- `AuthenticationService` authenticates by selected account type and rejects
  invalid credentials, inactive accounts, and role mismatches. Passwords are
  hashed by `PasswordHasher` using PBKDF2-HMAC-SHA256.
- `BookService` and `BookCopyService` own catalogue and physical-copy rules.
- `LoanService` owns loan lookup and renewal. `BorrowService` coordinates a
  multi-copy borrowing transaction and return operations, including copy status
  changes and overdue fine issuance.
- `ReservationService` validates member and book existence, requires an
  unavailable book, prevents duplicate pending reservations, applies the
  seven-day expiry period, and supports cancel, fulfil, expiry, and fair pending
  queue queries.
- `FineService` calculates overdue amounts at the configured daily rate, creates
  or reuses outstanding fines, supports member payment, and supports librarian
  edit/remove operations.
- `NotificationService` creates overdue alerts and reservation reminders
  idempotently, lists notifications in creation order, and persists read state.
- `LibrarianController` performs the active-librarian check before protected
  librarian actions and keeps user-facing errors at the view boundary.

Business rules belong in services or models. Controller methods should orchestrate
and authorize; JavaFX pages should validate only at the presentation boundary and
delegate persistence and business decisions.

## Cross-role integration contracts

The librarian implementation does not reach into member-owned repositories. It
uses stable IDs and these contracts in `libconnect.integration`:

| Contract | Purpose |
| --- | --- |
| `MemberDirectory` | Test whether a member exists and is active. |
| `MemberManagement` | Register, edit, activate/deactivate, reset password, and search members. |
| `BookCatalogue` | Test whether a book exists and whether it has an available copy. |
| `BookManagement` | Add, edit, remove, and search catalogue books. |
| `BookCopyManagement` | Record damaged and lost-copy incidents. |
| `LoanQuery` | Read loan summaries, active loans, and overdue loans. |
| `FineIssuer` | Let return logic issue an overdue fine without depending on fine storage. |

`LoanSummary`, `ReturnedLoanSummary`, `MemberSummary`, `BookSummary`, and
`BookDetails` are deliberately small cross-role data shapes. Extend a contract
only when the use case cannot be expressed through existing stable identifiers.
Contract changes need tests for valid, missing, duplicate, and invalid-ID cases.

The authorization boundary is `LibrarianService.requireActive`, called by
`LibrarianController` and the librarian page for protected actions. Member pages
also check that the current session contains a `Member` before member-only
operations.

## Persistence and data safety

Each entity type is stored as a JSON array:

| File | Entity | Identifier used by repositories |
| --- | --- | --- |
| `data/members.json` | `Member` | membership ID and user ID |
| `data/librarians.json` | `Librarian` | employee ID and user ID |
| `data/books.json` | `Book` | ISBN |
| `data/book-copies.json` | `BookCopy` | copy ID |
| `data/loans.json` | `Loan` | loan ID |
| `data/reservations.json` | `Reservation` | reservation ID |
| `data/fines.json` | `Fine` | fine ID |
| `data/notifications.json` | `Notification` | notification ID |

`StorageManager`:

1. Creates the data directory and a missing JSON file containing `[]`.
2. Deserializes records with Jackson and Java time support.
3. Skips malformed individual records, including duplicate identifiers, and
   appends their source file, entity type, record number, reason, and raw record
   to `data/malformed/malformed-records.json`.
4. Rejects an unreadable file or a root value that is not a JSON array with a
   `RepositoryException`.
5. Writes a complete replacement through a temporary file and replaces the
   destination after serialization succeeds. It attempts an atomic move and
   falls back to a normal replace when atomic moves are unsupported.

`AbstractFileRepository` reloads the file for every operation. It does not keep a
long-lived snapshot that could silently become stale. Repositories expose
`findById`, `findAll`, `save`, and `deleteById`; specialized repositories provide
queries such as `findByMemberId` or `findByStatus`.

Relationships are stored as IDs, not nested copies. For example, a loan stores a
member ID and copy ID, while a fine stores a loan ID and member ID. Do not edit
password hashes or change IDs manually in production data.

There is no general storage-level transaction or journal across all files.
`BorrowService` provides compensating rollback for its coordinated copy/loan
updates and tests cover its partial-failure behavior. Any new operation that
updates more than one repository must add equivalent rollback or a transaction/
journal mechanism before reporting success.

## Testing strategy

There are 66 Java test source files in the current repository. They cover:

- model invariants and transitions in `src/test/java/libconnect/models`;
- service rules and test doubles in `src/test/java/libconnect/services/unit`;
- cross-service and cross-role flows in
  `src/test/java/libconnect/services/integration`;
- librarian authorization, orchestration, and notification/fine behavior in
  the root `libconnect` test package;
- repository CRUD, round trips, malformed records, and temporary-directory
  isolation in `src/test/java/libconnect/storage` and
  `src/test/java/libconnect/storage/file/unit`;
- JavaFX page, component, navigation, and integrated librarian behavior in
  `src/test/java/libconnect/ui`.

Use `@TempDir` and injected repositories/clocks for persistence and date-sensitive
tests. UI tests use shared JavaFX helpers in `UiTestSupport`,
`UiPageTestSupport`, and `UiTestFixtures`; they must run with an initialized
JavaFX toolkit and a usable display.

Run:

```text
mvn test
mvn clean verify
```

`verify` creates `target/site/jacoco/index.html`. JaCoCo is diagnostic in the
current build: it exposes uncovered paths but is not configured as a percentage
gate. Prioritize domain, persistence failure, authorization, rollback, and
integration branches over layout-only getters.

For every code change:

1. Identify the owning role and inspect `PLAN.md` and `AGENTS.md`.
2. Search for an existing model, validator, clock, ID generator, repository
   method, or contract before creating a new helper.
3. Locate tests for the modified class. Add boundary, invalid-input, missing
   record, persistence, and integration coverage as appropriate.
4. Wire the smallest vertical slice through the composition root.
5. Run the full suite and inspect failures before handing off.

## Adding common kinds of changes

### Add a service capability

Put the invariant and validation at the service boundary. Inject repository
interfaces and a `Clock` when time affects behavior. Keep the service independent
of JavaFX and file paths. Add unit tests with in-memory doubles, then integration
tests if the capability crosses a repository or role boundary.

### Add persistence

Start with a repository interface in `storage/repositories`, then add the
corresponding `File...Repository` using `AbstractFileRepository` and
`StorageManager`. Persist stable IDs, test missing files, malformed records,
duplicate IDs, round trips, safe replacement failures, and temporary directories.
Wire the repository in the relevant composition root rather than constructing it
inside a page or domain model.

### Add a UI flow

Add or reuse a page/component, use exact visible labels and stable feedback, and
delegate actions to services or a thin controller. Protect member/librarian
actions at the session/controller boundary. Add a focused JavaFX test for
behavior not already covered by controller or integration tests, then run the
suite with a display.

## CI workflow

The current `.github/workflows/ci.yml` has two jobs:

1. `build` runs on `ubuntu-latest` for pushes and pull requests, checks out the
   repository, installs Temurin Java 25, and runs
   `mvn --batch-mode --update-snapshots clean verify`.
2. `release` runs only for tags whose ref starts with `v`, depends on `build`,
   installs Java 25, runs `mvn --batch-mode -Prelease clean package`, and uploads
   `target/libconnect-*.jar` as an artifact named `libconnect-<tag>`.

The workflow currently has no separate UI-test job, explicit virtual-display
setup step, coverage upload step, or test-report artifact upload step. Therefore
the JavaFX display requirement remains an environment risk for CI and should be
addressed in the workflow if `ubuntu-latest` cannot initialize the toolkit. Do
not document a CI artifact or job that is not present.

For a local CI-equivalent build, use JDK 25 and run:

```text
mvn --batch-mode --update-snapshots clean verify
```

If JavaFX fails before assertions with a screen/toolkit error, configure a
display-capable runner first. If Maven fails before compilation, inspect
`mvn --version` and ensure it reports JDK 25 rather than a JRE or an older JDK.

## Release process

The project version is `1.0.0`. The release profile:

- shades runtime dependencies into `libconnect-1.0.0.jar`;
- writes `libconnect.ui.LibConnectLauncher` into the JAR manifest;
- preserves service resources;
- copies the JAR to `release/libconnect-1.0.0.jar`.

Build it with:

```text
mvn -Prelease clean package
```

The JavaFX dependency classifier is selected by Maven profiles for Windows,
Linux, Linux aarch64, macOS, and macOS aarch64. Build on the target platform and
architecture because the shaded JAR includes platform-specific JavaFX native
libraries.

At runtime, data paths are relative to the process working directory. Distribute
the complete `data/` directory beside the directory from which the JAR is
started, and ensure it is writable. The application creates missing JSON files,
but it cannot reconstruct intentionally deleted records. Run a release manually
after building it and exercise both member and librarian workflows before
distribution.

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `No compiler is provided in this environment`. | Maven is using a JRE or an older Java installation. | Set JDK 25 as `JAVA_HOME`, verify `mvn --version`, and rerun. |
| `release` or normal compilation selects the wrong JavaFX binaries. | The build ran on a different OS/architecture or the expected Maven profile was not active. | Build on the target platform and inspect the active Maven profile/classifier. |
| JavaFX reports `Screen.getMainScreen` or hangs before assertions. | No usable display is available to the JavaFX toolkit. | Run on a desktop or configure a CI virtual display; do not treat it as a domain-test failure. |
| A repository reports a root JSON-array error. | A data file is not a JSON array. | Restore the file from backup or replace it with a valid array after preserving the evidence. |
| Some records disappear after loading. | Those individual records were malformed or had duplicate IDs. | Inspect `data/malformed/malformed-records.json`, repair the source records, and rerun. |
| A write fails or leaves the old data intact. | The directory is not writable or replacement failed. | Check permissions and free space; the safe-write path should leave the original file unchanged when replacement fails. |
| A new multi-file operation leaves partial state. | It has no rollback or transaction boundary. | Add compensating rollback or journal support before returning success. |
| The release starts but cannot find data. | It was started from a directory without the expected relative `data/` directory. | Start it from the directory containing `data/`, or place the complete data directory there. |

## Acknowledgements

This guide acknowledges and reuses the following project material and
dependencies:

- The feature list, package direction, persistence model, stable-ID relationships,
  and original architecture diagrams in [`PLAN.md`](../PLAN.md).
- The project ownership, engineering process, test gates, release notes, and
  current implementation status in [`AGENTS.md`](../AGENTS.md).
- The existing LibConnect implementation and tests in `src/main/java` and
  `src/test/java`, including the member/librarian integration contributed by the
  repository's recorded contributors.
- The project-local `authoring-user-guide` and `authoring-developer-guide`
  instructions, which state that their documentation approach is adapted from
  `bm629/agent-skills`.
- [OpenJFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson),
  [JUnit 5](https://junit.org/junit5/), [JaCoCo](https://www.jacoco.org/jacoco/),
  and the Maven plugins configured in [`pom.xml`](../pom.xml). These provide the
  runtime UI, JSON mapping, test framework, coverage instrumentation, packaging,
  and application launch support; their code is not copied into this repository.

No other external code, documentation, or design source was identified in the
repository search performed for this guide. Any future copied or adapted
material should be added to this section with its source and license.
