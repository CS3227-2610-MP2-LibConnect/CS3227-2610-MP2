---
name: authoring-user-guide
description: Author or update LibConnect's end-user guide from the implemented JavaFX workflows, exact UI labels, tests, and project documentation.
---

# Authoring the LibConnect User Guide

Use this skill for `docs/USER_GUIDE.md` and other end-user help for LibConnect. It
is adapted from the `authoring-user-guide` skill in
`bm629/agent-skills` and is scoped to this JavaFX desktop application.

## Audience and sources

Write for members and librarians who want to complete tasks, not for developers
who need to understand the implementation. Treat the following as the sources of
truth, in this order when they disagree:

1. Implemented JavaFX pages, components, controllers, and visible UI labels.
2. Passing UI/integration tests and their fixtures.
3. `PLAN.md`, `README.md`, and the existing user guide.
4. Manually verified behavior of the built application.

Never invent a screen, button, workflow, error message, or feature. If the
available sources do not settle a behavior, record it as an assumption or open
question instead of documenting it as fact.

## Authoring method

- Begin with a clear start-here path and state prerequisites such as Java 25,
  Maven, a desktop display, or a release JAR.
- Organize the guide around user goals: logging in, borrowing, reserving,
  returning, managing fines, reading notifications, and librarian operations.
- Provide one task-oriented how-to for each supported goal. Use numbered steps,
  one action per step, imperative voice, and exact labels such as `Login`,
  `Reservations`, or `Confirm Borrowing`.
- Keep the four documentation modes distinct: one happy-path tutorial,
  imperative how-to procedures, brief conceptual explanations, and a neutral
  feature reference.
- Write in plain language. Define product-specific terms and abbreviations on
  first use. Do not rely on color, screen position, or unexplained jargon.
- Document user-visible failure states as symptom -> likely cause -> fix. Use the
  actual validation and service messages where they are stable and verified.
- Include data-file and recovery guidance only at the level an end user needs;
  do not expose internal implementation details unnecessarily.

## Updating an existing guide

When a feature or UI changes, edit the affected sections in place. Search the
whole guide for old labels, removed workflows, changed expected results, and
stale troubleshooting entries. Update the guide's revision history when the
project's documentation convention provides one. Do not rewrite unrelated
sections or silently preserve conflicting instructions.

Before handing off, verify every documented workflow against the current source
and relevant tests, and run the appropriate local application or test command
when practical. Report any workflow that could not be verified.

## Output quality bar

A finished guide must be findable, task-oriented, understandable to a
non-technical reader, accurate to the current JavaFX UI, complete for the
supported member and librarian goals, and explicit about prerequisites and
known failure paths. It must not become an architecture guide or an API
reference.
