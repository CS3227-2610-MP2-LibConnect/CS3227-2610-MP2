---
name: authoring-developer-guide
description: Author or update LibConnect's internal developer guide from the Java architecture, Maven build, persistence design, tests, CI, and release workflow.
---

# Authoring the LibConnect Developer Guide

Use this skill for `docs/DEVELOPER_GUIDE.md` and related internal engineering
documentation. It is adapted from the
`authoring-developer-guide` skill in `bm629/agent-skills`; its original
SDK/API-adoption focus is deliberately changed here to fit LibConnect's JavaFX
desktop project.

## Audience and sources

Write for a developer who must build, test, extend, troubleshoot, or release
LibConnect. Use repository evidence rather than generic Java assumptions:

- `AGENTS.md` for project rules and ownership boundaries;
- `PLAN.md` for intended features and architecture;
- `README.md`, `pom.xml`, `.github/workflows/`, and release configuration;
- production packages, repository interfaces, composition roots, and data files;
- unit, integration, persistence, controller, and JavaFX UI tests.

Do not invent classes, commands, packages, CI jobs, or release behavior. Call out
gaps and assumptions explicitly. Keep user-facing task instructions in the user
guide; this guide may link to it rather than duplicate it.

## Required coverage

Keep the guide proportional to the project, with a clear start-here section and
accurate coverage of:

- Java 25 and Maven prerequisites plus repeatable build/test commands;
- package responsibilities and dependency direction;
- models, services, integration contracts, repositories, `StorageManager`, and
  JavaFX composition roots;
- stable-ID cross-role contracts and authorization boundaries;
- JSON persistence, malformed-record recovery, safe replacement, and data-file
  safety;
- test categories, UI-test constraints, integration boundaries, and coverage
  interpretation;
- GitHub Actions job dependencies, JavaFX environment requirements, diagnostic
  artifacts, and local CI-equivalent verification;
- release JAR creation, platform-specific JavaFX classifiers, runtime data
  placement, and acceptance testing;
- symptom -> cause -> fix troubleshooting for common build, test, display,
  persistence, and release failures.

Use short runnable commands and state their expected result. Link to source files
or tests when that is more useful than copying implementation details. Explain
concepts before procedures, and organize procedures around developer goals such
as adding a service, adding persistence, adding a UI flow, or preparing a
release.

## Updating an existing guide

Treat documentation changes as a scoped delta. First identify the changed source
or workflow, then search the guide for every affected command, class, package,
test path, label, and expected result. Update only the affected sections, keep
the architecture narrative internally consistent, and add a concise revision
note when the guide has a revision-history convention.

Before handing off, validate commands against the current `pom.xml` and workflow,
validate paths against the repository, and run relevant tests or checks when
practical. Report anything that remains unverified.

## Quality bar

A finished developer guide must help a new project developer build the project,
understand its boundaries, make a small compatible change, test it, diagnose
common failures, and produce a release artifact without relying on undocumented
tribal knowledge. It is an internal engineering guide, not a generic Java
tutorial, an endpoint catalog, or a user manual.
