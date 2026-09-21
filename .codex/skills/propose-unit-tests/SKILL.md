---
name: propose-unit-tests
description: Plan and iteratively refine unit-test cases for user-specified project files using boundary analysis, equivalence partitioning, and other sound test-design techniques; do not start without target file paths.
---

# Propose Unit Tests

Use this skill when the user wants a thoughtful unit-test plan for one or more specific files. The workflow is conversational and review-driven: first produce and refine the test-case list, then provide the final list. Do not silently implement tests or modify source/test files unless the user separately asks for implementation.

## Required safety gate

The user must identify the file or files to test. If no target files are provided, ask which file paths they want covered and stop. Do not create, edit, delete, rename, or format any files in that case. Do not infer a target from the repository or from a vague component name.

Once paths are provided, inspect the relevant source, public behavior, dependencies, existing tests, and build/test configuration read-only. Keep the scope to the specified files and their directly observable behavior unless the user expands it.

The user may also provide an initial list of proposed tests. If they do not, proceed directly to the independent test-design phase.

## Initial test-design pass

When the user provides proposed tests, do not look at that list while designing the first independent list. First analyze the target file(s) and generate the tests you consider sufficient, using applicable techniques such as:

- equivalence partitioning for valid, invalid, empty, null, and otherwise distinct input classes;
- boundary-value analysis for minimum, maximum, just-inside, just-outside, zero, one, and size/length limits;
- decision-table or pairwise coverage for interacting conditions;
- state-transition coverage for lifecycle or stateful behavior;
- error-path, exception, and failure-mode coverage;
- regression tests for observable defects or important invariants; and
- interaction/contract tests for collaborators when their behavior is observable and mocking is appropriate.

Choose focused tests that distinguish meaningful behavior. Avoid testing private implementation details, language/framework behavior, duplicate paths, or incidental formatting unless that is the file's contract. State assumptions when the source or specification is ambiguous.

For every independently generated test, include a concise name or scenario, inputs/preconditions, expected outcome, and a justification explaining the behavior or risk it covers.

## Analyze the user's proposed list

After the independent list is complete, compare it with the user's list. Examine every user-proposed test and report any test that is:

- out of scope for the specified file(s);
- not required because the behavior is not part of the contract or is already guaranteed elsewhere;
- a duplicate or subsumed by another test; or
- too implementation-specific to be a valuable unit test.

Highlight these tests clearly and explain the reason for each. Do not discard a user test merely because it is phrased differently: merge it when it covers a distinct behavior, or identify the exact overlapping coverage when calling it redundant.

## Proposed list and review loop

Present a consolidated proposed list containing every independently justified test plus all non-redundant, in-scope user tests. For each test, provide its scenario, expected result, and why it is required. Mark tests retained from the user's list when useful, but do not let that list influence the independent analysis retroactively.

End the proposal by asking the user for comments on the test cases. Do not proceed as if the user approved the list.

For each subsequent user response:

1. If the user proposes removing a test, assess the stated reason against the file's behavior and test coverage. Explain whether the reason is valid, partially valid, or invalid, and identify any coverage gap created by removal.
2. If the user proposes adding a test, analyze it using the same out-of-scope, required, duplicate, and implementation-specific checks. Explain whether it should be retained and why.
3. Rebuild the consolidated list, preserving valid changes and rechecking interactions and duplicates across the whole list.
4. Ask for comments again.

Repeat this loop until the user explicitly indicates they are satisfied or asks to finalize. If the user asks to finalize without resolving a disputed test, record the remaining assumption or disagreement beside that test rather than silently removing it.

## Final output

When the user is satisfied, provide the final test list grouped by target file or behavior. Include the test name/scenario, setup and inputs, expected result, and a short justification for each. Include a brief note of any intentionally omitted or user-requested-but-rejected tests and the reasons. This skill's default deliverable is the list and rationale; implementation requires a separate explicit request.
