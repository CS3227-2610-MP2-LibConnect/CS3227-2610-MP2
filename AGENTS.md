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

## Code Generation Practices

Before writing new helpers or logic, search the codebase for existing implementations that already solve the problem, and reuse or extend them rather than duplicating. Be alert to logic that is likely to be needed across multiple files (e.g., date calculations, validation, formatting) — these should live in a shared/common module rather than being reimplemented per file. If a similar method already exists but doesn't quite fit, prefer generalizing it over writing a near-duplicate. When duplication is found during a change, extract it into a shared helper as part of that change rather than leaving it for later. This is mandatory: Before making any changes to existing files for the purposes listed above, you are to inform the user and ask for permission before implementing the changes.

After creating any new classes or modifying any code, look through the repository for its corresponding test file. You are required to follow the following steps:

1. Look through the repository for any test files for the modified class.
1. If no test files are found highlight this to the user to inform them of the need to create unit tests.
1. If test files are found, inspect them to look for any missing new functionality created that may not be tested by the existing test suite. If there are any such instances, highlight them to the user.
1. Highlight any test cases that may have become redundant as a result of modification to the classes, for example, due to the removal of certain functionalities from existing classes.
1. Highlight any integration tests that may need to be added.
1. Run all existing tests, and highlight any failing tests to the user.
