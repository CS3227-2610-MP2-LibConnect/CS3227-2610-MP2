---
name: test-info
description: "Inspect one or more test files and write a project-root test_summary.md describing test type, dependencies, cases, inputs, pass/fail status, and coverage gaps."
---

# Test Info

Use this skill when the user asks what one or more test files contain or asks for a test-information summary. The user supplies the test-file paths or identifies the files in the current project.

## Required output behavior

- Do not present the test analysis in the chat.
- Write the complete report to `test_summary.md` in the root directory of the project being analyzed.
- If the file already exists, replace it with a fresh report rather than appending stale findings.
- Ensure `test_summary.md` is ignored by Git. Add an exact `test_summary.md` entry to the project-root `.gitignore` when it is absent. Preserve all existing ignore rules and unrelated changes.
- If the target project root or a supplied test file cannot be resolved, report the blocker in the output file when possible; do not invent findings.

## Analysis workflow

1. Resolve every supplied test-file path. If the user gives a directory, inspect the test files within that scope and state the selection rule in the report.
2. Read each test file completely. Inspect the production classes it imports, constructs, mocks, stubs, or calls so dependencies are based on code rather than names alone.
3. Determine the test type from its execution boundary and collaborators: unit, integration, system/end-to-end, repository/persistence, controller, or another appropriate category. A file may have more than one category; explain the distinction briefly.
4. Determine pass/fail status from available project evidence. Prefer a targeted test run for the supplied files, then use existing reports or recent command output if a run is not possible. Record the command, result, and any inability to execute. Do not claim that a test passes merely because its assertions look valid.
5. Identify each distinct test case currently implemented, including the behavior under test and the expected outcome. Include parameterized cases as a grouped case with their parameters or enumerate them when that is clearer.
6. Record explicit inputs used by each case: literals, fixtures, builders, generated values, dates, IDs, statuses, repository records, and relevant mocked return values. Name important fields such as name, email, password, ISBN, loan ID, or amount when present. For generated or inherited inputs, describe their source and representative value if known.
7. Separate project dependencies from external/framework dependencies. Project dependencies should identify the test's production classes, repositories, services, models, fixtures, data files, and collaborators, with paths where practical. External dependencies should include JUnit, Mockito, JavaFX, and similar libraries only when they affect interpretation.
8. Identify coverage gaps by comparing the tested behavior with the reachable public behavior and important boundary/error paths of the dependent production classes. Distinguish a real untested path from a path that is covered indirectly. Mention redundant or overlapping cases only when they materially affect test value.

## Report format

Create a concise Markdown report with:

- a title and generation date;
- a short scope and execution-status summary;
- one section per test file;
- for each file: test type, pass/fail status, project dependencies, external dependencies, implemented test cases, inputs, and potential testing gaps;
- a final cross-file observations section for shared gaps, duplicated coverage, or notable limitations.

Use tables when they make repeated fields easier to compare, but retain enough prose to explain dependencies and gaps. Include source paths and line numbers for important findings when available. Clearly label inferred information and execution failures.
