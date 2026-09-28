---
name: acceptance-testing
description: Run and report final end-to-end acceptance testing for LibConnect's integrated member and librarian desktop workflows and release artifact.
---

# LibConnect Acceptance Testing

Use this skill for final end-to-end verification after member and librarian
features are integrated, before a release or handoff, or when the user asks for
an acceptance checklist and evidence.

## Scope and sources of truth

Derive the acceptance matrix from `PLAN.md`, `docs/USER_GUIDE.md`,
`docs/DEVELOPER_GUIDE.md`, the current UI, and existing integration/UI tests.
Cover both roles and the boundaries between authentication, services,
repositories, file persistence, JavaFX pages, and notifications. Do not expand
the product scope or silently treat unit tests as end-to-end evidence.

## Preconditions and safety

- Record the OS, Java version, Maven version, commit or working-tree state, and
  whether the test is local or CI.
- Use Java SE 25 and a desktop-compatible display or the project's supported
  headless UI configuration.
- Use an isolated temporary data directory or test fixtures. Never use the
  repository's populated `data/` directory as disposable test state, and do not
  overwrite user records.
- Confirm that required seed accounts and records are available before testing.
  If credentials or fixtures are missing, record the blocker rather than
  fabricating them.

## Acceptance workflow

1. Run the complete automated gate with `mvn --batch-mode clean verify` and
   record the test count, failures, errors, skips, and generated reports.
2. Confirm application startup and login for both an active member and an active
   librarian. Verify invalid credentials and unauthorized access are rejected.
3. Exercise the member path: registration or seeded login, catalogue search,
   availability, borrowing, renewal where eligible, return, reservation,
   current/history views, fine visibility/payment, profile updates, and
   notification read state.
4. Exercise the librarian path: member management, book and copy management,
   reservation processing, fine create/edit/remove/view, overdue and reservation
   alerts, and dashboard refresh.
5. Verify cross-role effects: a member action is visible to the librarian, a
   librarian mutation is visible to the member, stable IDs remain consistent,
   and an overdue return creates the expected fine.
6. Restart or reload the application and verify successful mutations persist.
   Check malformed-record recovery and safe file replacement only when the test
   is isolated and the behavior is explicitly in scope.
7. Build the release artifact with `mvn -Prelease clean package` when release
   verification is requested. Run it from a writable directory with the required
   `data/` placement and verify the login window opens.
8. Recheck each failed or blocked case after the relevant fix. Do not convert a
   skipped scenario into a pass.

## Evidence and reporting

For each acceptance case, record the precondition, action, expected result,
actual result, evidence source, and status: Pass, Fail, Blocked, or Not run.
Separate automated coverage, manual UI coverage, persistence/restart coverage,
and release-artifact coverage. Include exact failing test names, messages, and
paths to reports or screenshots when available.

The final report must state the environment, commands run, overall result,
remaining gaps, and whether the project is ready for handoff. A green unit or UI
test suite is not sufficient evidence for a passing cross-role acceptance case
unless the integration behavior is actually covered by that test.

Do not modify production code, workflow files, or test fixtures while performing
acceptance testing unless the user separately requests implementation. If a
failure suggests a fix, report the smallest reproducible case and the affected
component.
