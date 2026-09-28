# LibConnect Developer Guide

This guide is for developers who build, extend, test, troubleshoot, and release
LibConnect. It describes the implemented JavaFX application and the reasoning
behind its current design. It is intentionally more detailed than a package
catalogue: before changing a boundary, read the trade-off and limitation notes
so that a local improvement does not undermine an intentional project-wide
decision.

The user-visible installation and workflow instructions are in the
[user guide](UserGuide.md). This document describes what is currently built,
including places where the implementation is deliberately smaller than a
production library-management platform.

## Table of contents

- [Start here](#start-here)
  - [Toolchain and prerequisites](#toolchain-and-prerequisites)
  - [Repeatable commands](#repeatable-commands)
- [Design at a glance](#design-at-a-glance)
  - [Package responsibilities](#package-responsibilities)
  - [Composition roots and object lifetime](#composition-roots-and-object-lifetime)
- [Architectural decisions and trade-offs](#architectural-decisions-and-trade-offs)
  - [File-backed JSON persistence](#file-backed-json-persistence)
  - [Read-through repositories instead of a long-lived cache](#read-through-repositories-instead-of-a-long-lived-cache)
  - [Stable IDs and small cross-role contracts](#stable-ids-and-small-cross-role-contracts)
  - [Services own policy; models own state transitions](#services-own-policy-models-own-state-transitions)
  - [Explicit composition over a dependency-injection framework](#explicit-composition-over-a-dependency-injection-framework)
  - [Compensating rollback for multi-file operations](#compensating-rollback-for-multi-file-operations)
  - [Tolerant record recovery with a malformed-record journal](#tolerant-record-recovery-with-a-malformed-record-journal)
  - [JavaFX pages with thin boundaries](#javafx-pages-with-thin-boundaries)
- [Domain, services, and authorization](#domain-services-and-authorization)
  - [Cross-role integration contracts](#cross-role-integration-contracts)
- [Persistence and data safety](#persistence-and-data-safety)
  - [Persistence limitations to keep visible](#persistence-limitations-to-keep-visible)
- [Testing strategy](#testing-strategy)
  - [What to test for each layer](#what-to-test-for-each-layer)
- [How to extend the system safely](#how-to-extend-the-system-safely)
  - [Add a service capability](#add-a-service-capability)
  - [Add persistence](#add-persistence)
  - [Add a UI flow](#add-a-ui-flow)
  - [Change a stable contract](#change-a-stable-contract)
- [CI workflow](#ci-workflow)
- [Release process](#release-process)
- [Future extensibility](#future-extensibility)
- [Planned Enhancements](#planned-enhancements)
- [Troubleshooting](#troubleshooting)
- [Source and test map](#source-and-test-map)
- [Acknowledgements](#acknowledgements)



## Start here



### Toolchain and prerequisites

- JDK 25, including `javac`.
- Maven 3.9 or later.
- A desktop-capable display for the JavaFX application and JavaFX UI tests.

The `[pom.xml](../pom.xml)` targets Java 25, JavaFX 25, JUnit 5.14.4, Jackson
2.22.2, and JaCoCo 0.8.14. Verify that both Java and Maven use JDK 25:

```text
java --version
javac --version
mvn --version
```

Expected result: Java and Maven report version 25 and `javac` is available. A
JRE-only Maven environment fails with `No compiler is provided in this environment`.

### Repeatable commands

Run the complete test suite:

```text
mvn test
```

Expected result: all unit, integration, persistence, controller, and JavaFX
tests pass.

Run a clean build, tests, and the JaCoCo report:

```text
mvn clean verify
```

Expected result: the build succeeds and the report is written to
`target/site/jacoco/index.html`. Coverage is currently diagnostic rather than a
percentage gate.

Run the application from the checkout:

```text
mvn exec:java
```

The configured main class is `libconnect.ui.LibConnectLauncher`. Expected
result: a LibConnect JavaFX window opens. The application resolves its data
files relative to the process working directory, so run this command from the
project root when using the checked-in `data/` directory.

## Design at a glance

LibConnect uses explicit layers with dependencies pointing toward domain rules
and interfaces:

```text
JavaFX pages and reusable components
                |
                v
controllers, session checks, and stable integration contracts
                |
                v
services and domain models
                |
                v
repository interfaces
                |
                v
JSON repository implementations -> StorageManager -> data/*.json
```

The central design goal is to keep business decisions testable without a JavaFX
toolkit or a real data directory. Models express state and invariants, services
own use-case rules, repositories abstract persistence, and composition roots
assemble the concrete application. Views format input and feedback but do not
decide whether an operation is allowed.

### Package responsibilities


| Package                           | Responsibility                                                                                      |
| --------------------------------- | --------------------------------------------------------------------------------------------------- |
| `libconnect.models`               | Domain entities, value validation, statuses, and state transitions.                                 |
| `libconnect.services`             | Authentication, users, catalogue, copies, borrowing, loans, reservations, fines, and notifications. |
| `libconnect.integration`          | Stable ID-based contracts and small data shapes between member and librarian functionality.         |
| `libconnect.storage.repositories` | Persistence abstractions and query contracts.                                                       |
| `libconnect.storage.file`         | JSON-backed repository implementations.                                                             |
| `libconnect.storage`              | Jackson mapping, file initialization, malformed-record recovery, and safe replacement.              |
| `libconnect.librarian`            | Librarian authorization/controller/view boundary.                                                   |
| `libconnect.ui`                   | JavaFX startup, navigation, session state, runtime composition, and dependency wiring.              |
| `libconnect.ui.pages`             | Login, registration, member, and librarian screens.                                                 |
| `libconnect.ui.components`        | Reusable cards, forms, navigation, feedback, and the librarian view adapter.                        |
| `libconnect.util`                 | Small shared validation utilities used across services.                                             |




### Composition roots and object lifetime

`LibConnectApplication.start` creates the top-level member-facing services used
by navigation, the session, the clock, and the `JavaFxLibrarianView`. It
delegates librarian wiring to `LibrarianCompositionRoot`, then passes the
resulting `LibrarianRuntime` to `SceneNavigator`. The authentication service
and several convenience service constructors create their own default
dependencies; runtime components therefore share the same data files rather
than a single in-memory repository instance.

`LibrarianCompositionRoot` is the file-backed composition root. It creates the
`StorageManager`, repositories, member-role adapters, librarian services,
`ReservationService`, `FineService`, `NotificationService`, and
`LibrarianController`. Its explicit parameters let tests substitute a temporary
data directory, a fixed `Clock`, and a test view. Keep construction here when a
dependency needs to be replaced in a test; do not construct repositories inside
pages or models.

`SceneNavigator` owns page transitions and checks the current session before
showing member or librarian workspaces. The authentication flow places the
active member or librarian returned by `AuthenticationService` in the session.
The controller and service checks remain necessary even when navigation already
hides a page: navigation is a UI convenience, not an authorization boundary.

## Architectural decisions and trade-offs

The following decisions are part of the current architecture, not universal
rules for every future version of LibConnect.

### File-backed JSON persistence

The application stores one JSON array per entity type under `data/`. This was
chosen because the project is a local desktop application with a small data
volume, and the records should be easy to inspect, seed, copy into a test
fixture, and recover without installing a database server.

Benefits:

- zero database setup for a student or local-library installation;
- human-readable records that make demonstrations and debugging practical;
- repository interfaces keep a future database implementation from leaking
into services;
- temporary directories make persistence tests isolated and repeatable.

Costs and rejected alternative:

- each save rewrites the complete entity file, so write cost grows with file
size and concurrent writers are unsafe;
- queries are in-memory scans after a file read, with no indexes or pagination;
- schema evolution and referential-integrity checks are application concerns;
- a relational database was rejected for the current scope because its setup,
migrations, and deployment model would add more operational complexity than
the local workflow requires.

If the dataset or deployment model changes, implement a new repository backend
behind the existing interfaces first. Do not make services know whether a query
is backed by JSON, SQL, or an in-memory test double.

### Read-through repositories instead of a long-lived cache

`AbstractFileRepository` reloads its file for every operation and writes a
complete snapshot after `save` or a successful delete. This favors freshness
and simple failure behavior over throughput: separate repository instances do
not silently retain stale copies of the same file.

A process-wide cache or an ORM identity map was not selected because this
application has one active desktop process and the dominant risk is accidental
stale state, not database latency. The trade-off is repeated parsing and full
file replacement. If caching is introduced later, it must include explicit
invalidation, a concurrency policy, and tests proving that a second reader sees
committed changes.

### Stable IDs and small cross-role contracts

Relationships are persisted as stable IDs rather than nested entity copies. A
loan stores a member ID and book-copy ID; a fine stores a loan ID and member ID.
Member-owned and librarian-owned code communicate through contracts in
`libconnect.integration`, such as `LoanQuery`, `MemberDirectory`,
`BookCatalogue`, and `FineIssuer`, plus small summaries such as `LoanSummary`
and `ReturnedLoanSummary`.

This avoids duplicating mutable member, book, and loan state and makes ownership
visible in the type system. The cost is that a service may need several lookups,
and contract changes require coordination and contract tests. Direct access to
another role's repository was rejected because it would couple persistence
details to the other role and make ownership boundaries impossible to enforce.

When adding a cross-role use case, first ask whether an existing stable ID and
summary are sufficient. Extend a contract only when the use case cannot be
expressed with existing data. Add tests for valid, missing, duplicate, and
invalid-ID cases whenever a contract changes.

### Services own policy and models own state transitions

Models such as `Loan`, `Reservation`, and `BookCopy` protect their own state
transitions. Services decide use-case policy: for example, `ReservationService`
checks member and book availability, prevents duplicate pending reservations,
and orders the pending queue, while the `Reservation` model represents valid
status changes.

This split keeps rules reusable from both the JavaFX UI and tests. A more
anemic model would make services easier to serialize but would allow callers to
construct invalid state. A fully rich domain model with domain events was not
adopted because the current application has few aggregates and no event bus;
introducing one now would obscure simple workflows. The limitation is that
services can still become large if unrelated policies are added without a new
boundary.

Use a new service or policy object when a rule needs its own dependency, clock,  
repository, or test matrix. Do not duplicate arithmetic or date policy in a  
controller and a service.

### Compensating rollback for multi-file operations

`BorrowService` validates all requested copies before writing, then coordinates
copy and loan updates. If a write fails, it deletes newly created loans and
restores original copy/loan snapshots. Return operations use the same
compensating approach and issue an overdue fine through `FineIssuer`.

This provides useful failure behavior without requiring a database transaction
manager. It is not a general atomic transaction: a process crash, disk failure,
or rollback failure can still leave files inconsistent. A storage-level journal
or transaction coordinator is the long-term solution if more workflows update
multiple files. Do not report success for a new multi-file operation until it
has an equivalent rollback or journal strategy.

### Tolerant record recovery with a malformed-record journal

`StorageManager` skips individual records that cannot deserialize or that repeat
an identifier, logs a warning, and writes the source file, entity type, record
number, reason, and raw record to `data/malformed/malformed-records.json`.
It fails the read when the file itself is unreadable or its root is not a JSON
array.

This lets valid records remain available after one damaged record while keeping  
the evidence needed for repair. A fail-fast parser would make corruption more  
visible immediately, but one bad record would make the whole feature  
unavailable. The current policy has a corresponding limitation: data can appear  
to disappear until the journal is inspected and the source record is repaired.  
The journal is a recovery aid, not a substitute for backups or schema  
validation.

## Domain, services, and authorization

The main service responsibilities are:

- `AuthenticationService` authenticates against the selected account type and
rejects invalid credentials, inactive accounts, and role mismatches. Passwords
are hashed with PBKDF2-HMAC-SHA256 through `PasswordHasher`.
- `BookService` and `BookCopyService` own catalogue and physical-copy rules.
- `LoanService` owns loan lookup and renewal. `BorrowService` coordinates
multi-copy borrowing and return operations, including copy status changes and
overdue fine issuance.
- `ReservationService` validates member and book existence, requires an
unavailable book, prevents duplicate pending reservations, applies the
configured expiry period, and supports cancellation, fulfilment, expiry, and
fair pending-queue queries.
- `FineService` calculates overdue amounts using a configured `BigDecimal` daily
rate, creates or reuses outstanding fines, supports member payment, and
supports librarian edit/remove operations.
- `NotificationService` creates overdue alerts and reservation reminders
idempotently, lists notifications by recipient, and persists read state.
- `LibrarianService` owns librarian registration, editing, deactivation, search,
and the `requireActive` authorization check.
- `LibrarianController` authorizes protected actions, delegates to services, and
translates failures to the `LibrarianView` boundary.

Business rules belong in models or services. Controllers should orchestrate and
authorize. JavaFX pages should validate obvious presentation input and delegate
business decisions. Never treat a hidden navigation item as sufficient
authorization.

### Cross-role integration contracts

Librarian services and controllers do not directly depend on member-owned
repositories. `LibrarianCompositionRoot` creates repository-backed adapters and
exposes the required member-role operations through these stable contracts in
`libconnect.integration`:


| Contract             | Purpose                                                                   |
| -------------------- | ------------------------------------------------------------------------- |
| `MemberDirectory`    | Check whether a member exists and is active.                              |
| `MemberManagement`   | Register, edit, activate/deactivate, reset password, and search members.  |
| `BookCatalogue`      | Check whether a book exists and has an available copy.                    |
| `BookManagement`     | Add, edit, remove, and search catalogue books.                            |
| `BookCopyManagement` | Record damaged and lost-copy incidents.                                   |
| `LoanQuery`          | Read loan summaries, active loans, and overdue loans.                     |
| `FineIssuer`         | Let return logic issue an overdue fine without depending on fine storage. |


The main limitation of these contracts is deliberate narrowness: they are good
for the current workflows, not a general remote API. Keep them stable and
small. If a future feature needs richer queries, prefer a new summary or
purpose-specific method over exposing a repository or a mutable entity graph.

## Persistence and data safety

Each entity type is stored as a JSON array:


| File                      | Entity         | Repository identifier / related ID                   |
| ------------------------- | -------------- | ---------------------------------------------------- |
| `data/members.json`       | `Member`       | user ID; membership ID is used by member operations  |
| `data/librarians.json`    | `Librarian`    | user ID; employee ID is used by librarian operations |
| `data/books.json`         | `Book`         | ISBN                                                 |
| `data/book-copies.json`   | `BookCopy`     | copy ID                                              |
| `data/loans.json`         | `Loan`         | loan ID                                              |
| `data/reservations.json`  | `Reservation`  | reservation ID                                       |
| `data/fines.json`         | `Fine`         | fine ID                                              |
| `data/notifications.json` | `Notification` | notification ID                                      |


`StorageManager` performs the shared file work:

1. It creates the data directory and initializes a missing entity file to `[]`.
2. It uses Jackson with Java time and constructor-parameter support.
3. It rejects an unreadable file or a root value that is not a JSON array with a
  `RepositoryException`.
4. It skips malformed individual records, including duplicate identifiers, and
  appends recovery details to the malformed-record journal.
5. It serializes a complete replacement to a temporary file, attempts an atomic
  move, and falls back to a normal replacement if atomic moves are unsupported.

The safe replacement protects readers from a partially written JSON file, but
it does not provide multi-process locking or cross-file atomicity. A leftover
temporary file after an unusual cleanup failure is recoverable; the original
destination remains the safe copy.

`AbstractFileRepository` reloads the file for each operation and exposes
`findById`, `findAll`, `save`, and `deleteById`; specialized repositories add
queries such as `findByMemberId`, `findByStatus`, or notification idempotency
checks. The repository contract treats `save` as create-or-replace by stable
ID. Do not edit password hashes, IDs, or related IDs manually in a running
installation without a backup and an understanding of the referential impact.

### Persistence limitations to keep visible

- There is one active application instance assumption; concurrent writers can
overwrite one another.
- A save rewrites the whole array, so large datasets will require a database or
indexed store.
- There is no schema version, migration runner, foreign-key enforcement, or
automated backup.
- A malformed record is skipped rather than repaired automatically.
- Data location is the process working directory, which is convenient for a
packaged demo but fragile for installed desktop applications.

These are extension points, not reasons to bypass the existing repository
interfaces. See [Future extensibility](#future-extensibility) before changing
the storage boundary.

## Testing strategy

The repository currently contains 102 production Java source files and 66 Java
test source files. Tests are organized by the boundary they protect:

- model invariants and state transitions in
`[src/test/java/libconnect/models](../src/test/java/libconnect/models)`;
- service rules and test doubles in
`[src/test/java/libconnect/services/unit](../src/test/java/libconnect/services/unit)`;
- cross-service and cross-role flows in
`[src/test/java/libconnect/services/integration](../src/test/java/libconnect/services/integration)`;
- librarian authorization, orchestration, and service behavior in the root
`libconnect` test package;
- repository CRUD, round trips, malformed records, and temporary-directory
isolation in
`[src/test/java/libconnect/storage](../src/test/java/libconnect/storage)` and
`[src/test/java/libconnect/storage/file/unit](../src/test/java/libconnect/storage/file/unit)`;
- JavaFX pages, components, navigation, and integrated librarian behavior in
`[src/test/java/libconnect/ui](../src/test/java/libconnect/ui)`.



### What to test for each layer


| Change                     | Minimum useful tests                                                                                      | Why                                                                      |
| -------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| Model or status transition | Valid transition, invalid transition, boundary values, and null/blank inputs.                             | Protects invariants independently of storage and UI.                     |
| Service policy             | Happy path, missing/duplicate records, invalid IDs, time boundaries, and dependency failure.              | Verifies the use case rather than the implementation sequence.           |
| Cross-role contract        | Valid, missing, duplicate, and invalid-ID cases, plus an integration flow.                                | Prevents one role from silently relying on another role's storage shape. |
| Repository                 | CRUD, replacement semantics, missing file, malformed record, duplicate ID, round trip, and write failure. | Protects data safety and compatibility with future backends.             |
| Multi-file operation       | Failure at each write point, rollback success, rollback failure, and no false success.                    | Demonstrates the transaction boundary rather than assuming it.           |
| JavaFX flow                | User-visible behavior, exact labels/feedback, navigation guard, and toolkit/display setup.                | Keeps UI tests focused on behavior that lower layers cannot observe.     |


Use `@TempDir` for file-backed tests and inject a fixed `Clock` for date or
time-sensitive behavior. UI tests use shared helpers in `UiTestSupport`,
`UiPageTestSupport`, and `UiTestFixtures`; they need an initialized JavaFX
toolkit and a usable display.

JaCoCo is useful for finding untested branches, but its current configuration
does not fail the build at a percentage threshold. Interpret coverage together
with risk: authorization, persistence failures, malformed data, rollback, and
cross-role boundaries deserve stronger tests than layout-only getters.

For every code change:

1. Search for an existing validator, clock, ID generator, repository method,
  contract, or shared UI helper before adding a new one.
2. Locate tests for the modified class. Add missing boundary and integration
  coverage rather than assuming `mvn test` proves the new behavior.
3. Wire the smallest vertical slice through the composition root.
4. Run `mvn test`, then `mvn clean verify` when the change affects packaging,
  coverage, or multiple layers.



## How to extend the system safely



### Add a service capability

1. Define the invariant and the service boundary before writing UI code.
2. Reuse existing domain transitions and validation helpers.
3. Inject repository interfaces, a `Clock`, and other external dependencies.
4. Return domain results or throw the existing service exceptions; do not expose
  JSON or JavaFX types.
5. Add unit tests with in-memory or fake dependencies, then integration tests if
  the capability crosses a repository or role boundary.
6. Register the service in the relevant composition root and add the UI only
  after the service behavior is green.



### Add persistence

Start with a repository interface in `libconnect.storage.repositories`. Add the
corresponding file repository by extending `AbstractFileRepository` and using
`StorageManager`. Persist stable IDs, define specialized query semantics, and
test missing files, malformed records, duplicate IDs, round trips, replacement
failure, and temporary directories. If the entity participates in a
multi-file operation, document the rollback or journal boundary before wiring
the service.

Do not put file paths in models or create a repository inside a JavaFX page.
For application wiring, the composition root should select the concrete backend
so all related services share the intended data directory and test doubles can
be substituted consistently. Convenience constructors on several services
currently create the default file-backed repositories; keep those constructors
for simple standalone use, but do not use them when wiring a multi-service flow
that requires shared or isolated dependencies.

### Add a UI flow

Add or reuse a page/component, use exact visible labels and stable feedback, and
delegate actions to a service or thin controller. Protect member/librarian
actions at the session and service/controller boundaries. Add a focused JavaFX
test for behavior not already covered below the UI layer, then run the suite
with a display.

### Change a stable contract

Treat `libconnect.integration` as an ownership boundary. Prefer adding a
purpose-specific method or immutable summary over exposing a repository or
returning mutable domain graphs. Update both sides, add contract tests for
missing and invalid identifiers, and run the cross-role integration suite before
changing any page.

## CI workflow

The current `[.github/workflows/ci.yml](../.github/workflows/ci.yml)` has two
jobs:

1. `build` runs on `ubuntu-latest` for pushes and pull requests, checks out the
  repository, installs Temurin Java 25, and runs
   `xvfb-run --auto-servernum --server-args="-screen 0 1920x1080x24" mvn  --batch-mode --update-snapshots clean verify`. The virtual display is needed
   for JavaFX UI tests.
2. `release` runs only for tags whose ref starts with `v`, depends on `build`,
  installs Java 25, runs the release build in the same virtual display, and
   uploads `target/libconnect-*.jar` as an artifact named
   `libconnect-<tag>`.

The release job cannot run when the build gate fails. There is no separate UI
job, coverage upload, test-report artifact upload, or published release step.
The full Maven suite is therefore the authoritative automated gate, while
manual acceptance remains necessary for a packaged desktop application.

For a Linux CI-equivalent run:

```text
xvfb-run --auto-servernum --server-args="-screen 0 1920x1080x24" mvn --batch-mode --update-snapshots clean verify
```

On a desktop OS with a usable display, the `xvfb-run` wrapper is not required.
If JavaFX fails before assertions with a screen or toolkit error, diagnose the
display first; it is not evidence of a domain-test failure.

## Release process

The project version is `1.0.0`. The `release` Maven profile:

- shades runtime dependencies into `libconnect-1.0.0.jar`;
- writes `libconnect.ui.LibConnectLauncher` into the JAR manifest;
- preserves service resources;
- copies the result to `release/libconnect-1.0.0.jar`.

Build it with:

```text
mvn -Prelease clean package
```

The default JavaFX classifier is `win`; Maven profiles select `linux`,
`linux-aarch64`, `mac`, or `mac-aarch64` on the corresponding platforms and
architectures. Build on the target platform and architecture because the
shaded JAR includes platform-specific JavaFX native libraries. A single JAR is
not automatically portable across all operating systems and architectures.

At runtime, data paths are relative to the process working directory. Distribute
the complete `data/` directory beside the directory from which the JAR is
started, ensure it is writable, and back it up before first use. The application
creates missing JSON files but cannot reconstruct intentionally deleted records.

Before distribution, build the release, start it from a clean staging directory
containing the intended `data/` directory, and exercise both member and
librarian workflows. The [acceptance-testing skill](../.codex/skills/acceptance-testing/SKILL.md)
defines the project's final end-to-end checklist when a release-level handoff is
required.

## Future extensibility

The current boundaries are designed to make these changes possible without
rewriting the application layer:


| Future need                     | Preferred extension                                                                 | Important trade-off                                                                                                 |
| ------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| Larger or shared deployment     | Add database-backed implementations of repository interfaces.                       | Gains indexing, concurrency, and transactions but requires migrations, deployment, and backup operations.           |
| Cross-file atomicity            | Add a storage transaction/journal coordinator used by services.                     | Improves crash recovery but adds recovery states, journal cleanup, and failure-mode testing.                        |
| Schema evolution                | Add a schema version and explicit migration steps before deserialization.           | Makes upgrades safer but requires versioned fixtures and rollback plans.                                            |
| Installed desktop data location | Inject a platform-aware data-directory provider.                                    | Avoids working-directory fragility but requires OS-specific conventions and migration from existing relative paths. |
| Scheduled alerts and expiry     | Add an application scheduler or explicit maintenance command that invokes services. | Removes reliance on a user opening a page, but adds lifecycle, duplicate-run, and shutdown concerns.                |
| Multiple concurrent clients     | Add locking or move to a server/database boundary.                                  | File-level improvements alone cannot provide reliable distributed consistency.                                      |


The safest migration sequence is to preserve service and integration contracts,
introduce the new implementation behind an interface, run old and new
implementations against fixtures, migrate data explicitly, and only then change
the composition root. Avoid adding a second direct path to the same data files;
that would create two sources of truth.

## Planned Enhancements

LibConnect currently delivers the core member and librarian workflows. 
The following enhancements represent optional future extensions
beyond that core functionality. They require additional hardware, third-party
services, or cross-aggregate workflows, so they should be designed as separate
vertical slices with new contracts and failure-mode tests.

- **Barcode and RFID scanning:** Allow members or librarians to scan a book's
barcode or RFID tag instead of entering a copy ID manually. This would need
compatible scanning hardware and an input adapter that converts the scan into
an existing stable copy identifier.
- **Book-return verification:** Integrate a bookdrop or sorting system to
verify returned books. The current return flow marks the loan returned and
the copy available after the member selects `Return Loan`; hardware-assisted
verification would need an event boundary, reconciliation for failed scans,
and a policy for books whose physical condition differs from the record.
- **Reservation fulfilment and loan tracking:** Create a loan automatically
when a librarian fulfils a reservation, with the selected copy and member
linked in one coordinated operation. Currently, fulfilment changes only the
reservation status; it does not check out a copy or create a loan, so the
librarian is expected to set aside a copy before sending a collection
reminder, while loan creation remains a separate borrowing workflow. The
enhancement would need a cross-file transaction or journal and a clear rule
for unavailable copies.
- **Real fine payments:** Integrate a payment gateway so `Pay Fine` represents
a confirmed external payment rather than only changing the local fine status.
The current implementation records the fine as paid without contacting a
payment provider. A future implementation should use an outbox or payment
adapter, idempotency keys, reconciliation, and secure handling of provider
failures.
- **Book reviews:** Allow members to leave reviews for books. This would add a
review model, repository, service, and UI flow, along with product decisions
for one-review-per-member rules, editing, moderation, visibility, and rating
aggregation.

These enhancements should preserve the existing service, repository, and
stable-ID boundaries. They should not be implemented by placing hardware calls,
payment-provider calls, or review policy directly in JavaFX pages.

## Troubleshooting


| Symptom                                                           | Likely cause                                                                         | Fix                                                                                              |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------ |
| `No compiler is provided in this environment`.                    | Maven is using a JRE or an older Java installation.                                  | Select JDK 25, verify `mvn --version`, and rerun.                                                |
| JavaFX reports `Screen.getMainScreen` or hangs before assertions. | No usable display is available to the JavaFX toolkit.                                | Run on a desktop or configure a virtual display with the CI command.                             |
| The build selects the wrong JavaFX binaries.                      | The build ran on a different OS/architecture or the expected profile was not active. | Build on the target platform and inspect the active classifier/profile.                          |
| A repository reports that the root must be a JSON array.          | A data file is not an array, often after manual editing or corruption.               | Preserve a copy, repair or restore the file, then rerun; do not silently replace evidence.       |
| Records disappear after loading.                                  | Individual records are malformed or have duplicate IDs and were skipped.             | Inspect `data/malformed/malformed-records.json`, repair the source records, and reload.          |
| A write fails but the old data remains.                           | The directory is not writable, disk space is unavailable, or replacement failed.     | Check permissions and free space; safe replacement is intended to preserve the old file.         |
| A new multi-file operation leaves partial state.                  | It has no rollback or journal boundary, or rollback itself failed.                   | Add compensating rollback and failure-point tests before reporting success.                      |
| A second application overwrites changes from the first.           | The JSON backend has no multi-process locking or transaction isolation.              | Use one active instance or move the operation to a transactional backend.                        |
| The release starts but cannot find data.                          | It was started from a directory without the expected relative `data/` directory.     | Start it from the staging directory containing `data/`, or package the data directory beside it. |
| Fine or notification behavior changes across runs.                | The system clock is used in runtime wiring or an idempotency/reference rule changed. | Inject a fixed `Clock` in tests and verify repository reference queries and date boundaries.     |




## Source and test map

Useful starting points for investigation:

- `[LibrarianCompositionRoot.java](../src/main/java/libconnect/ui/LibrarianCompositionRoot.java)`
for file-backed dependency wiring;
- `[StorageManager.java](../src/main/java/libconnect/storage/StorageManager.java)`
and `[AbstractFileRepository.java](../src/main/java/libconnect/storage/file/AbstractFileRepository.java)`
for persistence behavior;
- `[BorrowService.java](../src/main/java/libconnect/services/BorrowService.java)`
for compensating multi-file updates;
- `[ReservationService.java](../src/main/java/libconnect/services/ReservationService.java)`,
`[FineService.java](../src/main/java/libconnect/services/FineService.java)`, and
`[NotificationService.java](../src/main/java/libconnect/services/NotificationService.java)`
for librarian-owned policy;
- `[LibrarianController.java](../src/main/java/libconnect/librarian/LibrarianController.java)`
for protected librarian actions;
- `[LibrarianPageIntegrationTest.java](../src/test/java/libconnect/ui/LibrarianPageIntegrationTest.java)`
for an end-to-end JavaFX-to-persistence flow;
- `[LibraryServicesIntegrationTest.java](../src/test/java/libconnect/services/integration/LibraryServicesIntegrationTest.java)`
for cross-service integration behavior;
- `[StorageManagerTest.java](../src/test/java/libconnect/storage/unit/StorageManagerTest.java)`
and the file repository tests for malformed records and safe replacement;
- `[BorrowServiceTest.java](../src/test/java/libconnect/services/unit/BorrowServiceTest.java)`
for failure-point and rollback behavior.



## Acknowledgements

This guide acknowledges and reuses the following project material and
dependencies:

- The feature list, package direction, persistence model, stable-ID
relationships, and architecture diagrams in `[PLAN.md](../PLAN.md)`.
- The project ownership, engineering process, test gates, release notes, and
current implementation status in `[AGENTS.md](../AGENTS.md)`.
- The existing LibConnect implementation and tests in `src/main/java` and
`src/test/java`.
- The project-local `authoring-user-guide` and `authoring-developer-guide`
instructions, whose documentation approach is adapted from
`bm629/agent-skills`.
- [OpenJFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson),
[JUnit 5](https://junit.org/junit5/), [JaCoCo](https://www.jacoco.org/jacoco/),
and the Maven plugins configured in `[pom.xml](../pom.xml)`. These provide the
runtime UI, JSON mapping, test framework, coverage instrumentation, packaging,
and application launch support; their code is not copied into this repository.

No other external code, documentation, or design source was identified in the
repository search for this guide. Future copied or adapted material should be
added here with its source and license.