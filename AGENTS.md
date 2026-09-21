## Project Context

This is the repository for a greenfield java project for a Java Desktop Application LibConnect, a library system that allows for book inventory and loan management. Refer to the Features section in [`PLAN.md`](PLAN.md) for the specific features available to each of the user types.

## Default User Context

You are a developer working to develop LibConnect, a Java Desktop application using the Java SE 25 Version. You are to refer to [`PLAN.md`](PLAN.md) for the overall architecture design of the application. 

## Guidelines to Interact with the User

Explain the rationale for significant actions: what you did and why.
Keep explanations brief but instructive. For example:

  * Make generated code as self-explanatory as possible, and include explanatory comments where they improve understanding.
  * When faced with a design choice, choose the simplest option that is sufficient for the requirements, while briefly explaining relevant more advanced alternatives, unless otherwise stated by the user.

### Guidelines For Planning with the User

Whenever the user asks for suggestions on how to implement a particular component, you are to follow the project skill [`./codex/skills/plan-with-me/SKILL.md`] to aid the user in the planning process.

## Git standard

For every future commit in this project, follow the project skill [`.codex/skills/seedu-git-standard/SKILL.md`](.codex/skills/seedu-git-standard/SKILL.md). This is mandatory: before committing, review the staged diff and ensure the branch name, commit subject, and when the commit is non-trivial—commit body comply with the SE-EDU Git conventions. Do not create a non-compliant commit unless the user explicitly instructs otherwise.

## Coding Standard

Whenever code is generated for this project, follow the project skill [`.codex/skills/seedu-java-coding-standard/SKILL.md`](.codex/skills/seedu-java-coding-standard/SKILL.md). This is mandatory: Always create a javadoc for each method added, unless explicitly stated by the user to not include a javadoc.

## Librarian-Role Implementation Plan

This plan is limited to librarian-owned functionality from `PLAN.md`. Do not implement
member-owned authentication, member registration, catalogue search, book models,
book-copy models, or loan/borrowing rules except where a small, documented interface
contract is required for integration.

### Scope and ownership

The librarian role owns the following deliverables:

- `Librarian` model and librarian authorization checks.
- Reservation management: create/cancel/fulfil, pending-reservation processing, and
  reservation queries needed by librarians.
- Fine management: create, view, edit, remove, calculate, and validate fines.
- Notification management: overdue alerts, reservation reminders, querying, and read
  state changes.
- Librarian-facing service orchestration, controller actions, and views for the above
  capabilities.
- Librarian-owned repository interfaces and file-backed implementations for
  `Librarian`, `Reservation`, `Fine`, and `Notification`.

The member-role developer owns shared authentication, `User`/`Member`, `Book`/
`BookCopy`, catalogue logic, and loan/borrowing logic. Cross-role work must use
stable IDs and repository/service interfaces rather than reaching into another
role's implementation.

### Design alternatives

#### Approach A: Layered repositories and services (recommended)

Implement the architecture already described in `PLAN.md`: models contain domain
state and invariants, repository interfaces abstract persistence, file repositories
handle serialization, services enforce use-case rules, and controllers/views handle
interaction.

Advantages:

- Strong separation of concerns and dependency inversion.
- Services can be tested with in-memory repository fakes without file I/O.
- File persistence can later be replaced without changing librarian use cases.
- Small, reviewable increments with clear integration boundaries.

Risks and costs:

- More interfaces and classes are needed before the first end-to-end feature.
- Cross-file operations need explicit rollback or transaction handling.

This is the better choice for this project because `PLAN.md` already specifies this
architecture and the librarian features coordinate several persistent entities.

#### Approach B: Feature-first librarian vertical slices

Implement each feature as a self-contained slice, such as a reservation package
containing its model, persistence, service, controller, and view, then repeat for
fines and notifications.

Advantages:

- Each feature can reach an end-to-end demonstrable state quickly.
- Related code is easy to locate while a feature is being developed.

Risks and costs:

- Shared storage and cross-feature rules can be duplicated.
- Dependencies between reservations, loans, fines, and notifications become harder
  to standardize.
- It diverges from the package structure and repository boundaries in `PLAN.md`,
  increasing integration and migration cost.

Use this approach only if the team explicitly prioritizes independent feature
delivery over alignment with the planned architecture.

The team confirmed Approach A before implementation. Continue using the layered
repository/service design unless the team explicitly approves a different approach.

### Engineering rules for every implementation step

- Work in small vertical increments and keep each change independently buildable.
- Apply single responsibility, encapsulation, dependency inversion, and least
  privilege; keep business rules out of views and file-format details out of models.
- Depend on interfaces in services and inject repositories and clock/time sources
  where deterministic tests require them.
- Persist stable IDs instead of nested copies of related entities.
- Validate input and business invariants at service boundaries; return clear domain
  errors without exposing storage details.
- Use safe file replacement and ensure multi-repository updates either roll back or
  use a transaction/journal mechanism before reporting success.
- Target Java SE 25, follow the SE-EDU Java standard, and add Javadoc for every new
  public class and public method as required by this file.
- Do not mix unrelated refactoring with a feature implementation.

### Basic implementation todos and test gates

Each implementation item below has a mandatory test gate. A step is complete only
when its tests pass and the full existing test suite remains green.

1. **Bootstrap the project.** Add the Java SE 25 build configuration, source/test
   layout, test framework, and a repeatable test command. Add a smoke test proving
   the project compiles and the test runner executes.
   Test gate: run the smoke test and the complete test suite.

2. **Define shared contracts.** Agree with the member-role developer on stable ID
   types, status enums, repository result/error conventions, and the minimal loan
   query interface needed for overdue alerts and fine calculation. Document the
   contract before implementing either side.
   Test gate: add contract tests for valid, missing, duplicate, and invalid-ID cases.

3. **Implement librarian and librarian persistence.** Add the `Librarian` model,
   authorization/status rules, repository interface, file repository, and safe
   `StorageManager` integration. Initialize missing data files without destroying
   existing records.
   Test gate: test model invariants, repository CRUD, malformed records, missing
   files, safe replacement failure, and round-trip serialization using temporary
   directories.

4. **Implement reservation management.** Add the reservation model and repository,
   then implement `ReservationService` for creation, cancellation, fulfilment,
   expiry, duplicate prevention, and pending-reservation queries. Coordinate with
   the loan/book interfaces only through stable IDs.
   Test gate: test state transitions, expiry boundaries, duplicate reservations,
   missing members/books, ordering of pending reservations, and persistence.

5. **Implement fine management.** Add the fine model and repository, then implement
   `FineService` for creation from overdue loans, amount calculation, editing,
   removal, and member queries. Keep calculation policy in one service and avoid
   duplicated arithmetic in controllers or views.
   Test gate: test zero/one/many overdue days, amount boundaries, invalid amounts,
   missing loans/members, edit/remove behavior, and repository failure handling.

6. **Implement notifications.** Add the notification model and repository, then
   implement `NotificationService` for overdue alerts, reservation reminders,
   querying, and read-state changes. Make alert generation idempotent so retries do
   not create duplicates.
   Test gate: test message creation, recipient isolation, duplicate suppression,
   unread/read transitions, missing records, and persistence round trips.

7. **Add librarian orchestration.** Implement librarian-facing service methods for
   viewing overdue loans, loans, reservations, and fines, and for triggering the
   appropriate alerts/reminders. Keep orchestration thin and delegate rules to the
   domain services.
   Test gate: use mocked or in-memory dependencies to verify call ordering,
   authorization, partial-failure behavior, and no writes after a failed operation.

8. **Add the librarian controller and view.** Expose only authorized librarian
   actions, validate user input at the boundary, and keep presentation code free of
   persistence and business rules. Add accessibility-friendly error and success
   states where the UI framework permits.
   Test gate: controller tests for valid/invalid commands, unauthorized access,
   service errors, and successful refresh; add focused view tests only for behavior
   not covered by controller tests.

9. **Harden integration and persistence.** Exercise cross-file reservation, fine,
   loan-query, and notification flows. Add rollback/journal handling for operations
   that update multiple files and verify compatibility with member-role contracts.
   Test gate: integration tests against isolated temporary data directories,
   injected I/O failures, restart/reload behavior, and idempotent retries.

10. **Final verification and handoff.** Run formatting/static checks, the complete
    unit and integration test suites, and a manual librarian acceptance checklist
    covering every librarian feature in `PLAN.md`. Review public API Javadocs,
    dependency direction, error messages, and file safety before merging.
    Test gate: all automated checks pass with no ignored or quarantined failures.

### Definition of done for librarian work

- Every librarian feature in `PLAN.md` has an implementation, focused unit tests,
  and an integration test where it crosses a repository or role boundary.
- Every implementation step passed its test gate before the next step started.
- No member-owned code was changed beyond an agreed interface contract.
- Persistence failures cannot silently report success or leave known inconsistent
  records.
- Java SE 25 and the project coding/documentation standards are satisfied.
- The full test suite passes and the librarian acceptance checklist is complete.

### Current implementation status

- Completed: Java SE 25 Maven bootstrap, JUnit test setup, librarian model and
  persistence, reservation service, fine service, notification service, integration
  contracts, and authorized librarian controller boundary.
- Completed: focused model, repository, service, controller, and persistence tests;
  the current suite must remain green after every subsequent change.
- Pending: implementation of the member-role providers behind `MemberManagement`,
  `BookManagement`, `BookCopyManagement`, and `LoanQuery`; concrete desktop view
  wiring; and any cross-file transaction/journal logic required once those providers
  coordinate multi-repository writes.
- Pending: final end-to-end acceptance testing after both roles are integrated.
