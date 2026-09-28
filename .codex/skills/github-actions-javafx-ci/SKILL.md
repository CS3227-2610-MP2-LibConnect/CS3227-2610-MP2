---
name: github-actions-javafx-ci
description: Diagnose, repair, and verify LibConnect's GitHub Actions workflow for Maven, Java 25, JavaFX UI tests, reports, artifacts, and release ordering.
---

# GitHub Actions and JavaFX CI

Use this skill when inspecting or changing `.github/workflows/`, CI failures,
JavaFX test execution in runners, or the relationship between the full Maven
test suite and a UI-test job.

## Repository context

LibConnect targets Java SE 25, uses Maven and JUnit 5, and contains JavaFX page,
component, and integration tests under `src/test/java/libconnect/ui`. The
workflow and `pom.xml` are the source of truth for actual job names, profiles,
test selection, JavaFX classifiers, and report locations. Do not assume a UI job
exists: inspect the current workflow first.

## Diagnosis workflow

1. Read the complete workflow and `pom.xml` before editing either file.
2. Map each job to the Maven phase and test subset it runs. Identify whether a
   job is a full-suite gate, a UI subset, a release job, or diagnostics only.
3. Check job dependencies explicitly. A job that must wait for the full suite
   must use `needs: <full-suite-job>` or the appropriate dependency list; do not
   infer completion from log text, elapsed time, polling, or a shell sleep.
4. Check failure semantics. Use `always()` or step-level `if: always()` only when
   collecting diagnostics after failure is intended; do not accidentally allow a
   release or deployment to proceed after a failed test gate.
5. Make JavaFX execution deterministic for the runner: use the correct Java 25
   distribution, OS classifier/profile, display or headless configuration, and
   test JVM properties required by the existing UI tests.
6. Preserve machine-readable Surefire/Failsafe results and upload logs,
   screenshots, reports, or other failure evidence as artifacts when useful.
7. Keep local verification equivalent to CI: prefer `mvn --batch-mode clean
   verify` and targeted `-Dtest=...` commands over a separate untested script.

## Design rules

- Keep one authoritative full-suite job. If UI tests are split out, make the
  relationship explicit and ensure tests are not silently omitted or run twice
  without a reason.
- Use `needs` for ordering and result propagation. Use job outputs only for
  explicit data, not as a substitute for a dependency.
- Separate test execution from diagnostic upload so failed runs retain evidence.
- Pin action versions consistently with the existing workflow and keep permissions
  least-privileged.
- Do not change application behavior to hide CI instability. First identify
  whether the fault is workflow ordering, test discovery, JavaFX display setup,
  resource contention, or a real product/test failure.
- Do not use repository `data/` as a disposable test directory. UI and integration
  verification must use the fixtures or temporary storage already provided by the
  project.

## Verification and handoff

After a workflow change, run the closest local equivalent available, inspect test
reports, and validate the YAML structure. Confirm that:

- the intended job waits for the intended prerequisite;
- a failed full suite cannot produce a successful release artifact;
- UI tests are discovered and run exactly as intended;
- diagnostic artifacts are retained on failure;
- local Maven verification remains green.

Report the changed dependency graph, the verification commands and results, and
any runner limitation that could not be reproduced locally. Do not claim a CI
fix is complete without evidence from workflow validation or a clearly stated
local-only limitation.
