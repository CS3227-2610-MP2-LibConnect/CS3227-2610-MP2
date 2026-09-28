# Reflections on Basic Agentic Software Engineering

## 1. The `authoring-user-guide` skill & The `authoring-developer-guide` skill

One task we customized the Agent to perform were writing the user guide
and developer guide. Although both tasks involved Markdown, they needed
different audiences and different sources of truth. The user guide had to help
members and librarians use LibConnect. The developer guide had to help
developers understand the architecture, persistence, testing, CI, and release
process.

At the beginning, we asked the Agent to generate the user guide without using
the user-guide skill. The result was quite vague and unstructured. It described
LibConnect generally, but it did not give users a clear path through specific
tasks. It also did not consistently use the actual button names, field labels,
or error messages. This created additional work because we had to review the
whole document and identify what was missing or too general.

We then customized the `authoring-user-guide` skill for LibConnect's JavaFX
workflows. The skill was adapted from an upstream guide, but it was narrowed to
the actual application and its exact UI labels. It told the Agent to use the
implemented pages, visible labels, passing UI or integration tests, and project
documentation as sources of truth. It also required task-oriented steps and
real failure messages.

We could tell that the skill was working because the guide became more
specific and easier to compare with the application. For example, it explains
the sequence of selecting `Borrow books`, entering a value in `Enter book copy
ID`, selecting `Add Book`, and selecting `Confirm Borrowing`. It also includes
messages such as `Invalid email or password.` and `A transaction cannot contain
more than 10 books.` The guide became something a real user could follow,
rather than just a description of the product.

The developer-guide skill was customized in a similar way, but for internal
engineering work. The Agent was instructed to inspect `AGENTS.md`, `PLAN.md`,
`pom.xml`, workflows, source packages, repositories, composition roots, and
tests. This kept the guide connected to the actual LibConnect implementation
instead of turning it into a generic Java or Maven tutorial. This was effective 
for productivity because the Agent could collect details from many parts 
of the repository and organize them into the two guides. 


## 2. The `acceptance-testing` skill

The acceptance-testing skill was customized for a different task: checking
whether the integrated member and librarian application worked as a complete
product. We chose it because unit tests and UI tests alone do not prove that
login, JavaFX pages, persistence, cross-role actions, and the packaged release
work together.

The skill defines success more carefully than simply saying that the tests
passed. It asks us to record the environment, Java and Maven versions,
working-tree state, test mode, test data, expected result, actual result,
evidence source, and status. It also separates automated tests, manual UI
tests, persistence/restart tests, and release-artifact tests.

The logs show how this helped in practice. We re-ran
`mvn --batch-mode clean verify` and recorded 527 tests passing with 0 failures,
0 errors, and 0 skips. We rebuilt the release artifact with the release Maven
profile and verified that the shaded JAR had the expected main class and Java
25 metadata. We also launched it from an isolated temporary directory and
confirmed that it reached JavaFX startup without an application exception
during the smoke-test interval.

The safety requirements were useful too. We used isolated temporary data and a
temporary JavaFX cache instead of treating the repository's populated `data/`
directory as disposable. If we repeated the task, we would prepare the seed
accounts, isolated fixtures, and a real desktop session earlier. A tool that
collects test output, screenshots, environment details, and acceptance statuses
in one place would make the process more efficient.

## 3. `propose-unit-tests` and `seedu-git-standard`

The `propose-unit-tests` skill is useful for deciding what to test before
writing tests. It asks the Agent to work from specific file paths and to use
boundary analysis, equivalence partitioning, state transitions, failure paths,
and collaborator interactions. This gives the Agent a clear scope and helps it
focus on meaningful behaviour instead of generating many repetitive tests. We added 
tests for rollback, authorization, persistence, routing, notification state,
boundary dates, service failures, borrowing errors, reservation validation,
fine boundaries, librarian loan filtering and etc. 

The Agent was effective at organizing these test ideas, but it still needed us
to decide the actual scope and judge whether a test represented a real risk.
Otherwise, it could produce a neat-looking list that missed the most important
integration boundary. We would improve the process by adding a tool that
compares proposed tests with existing tests and reports duplicate cases or
coverage gaps.

The `seedu-git-standard` skill complemented the test-planning skill by making
the work easier to track and review. It defines conventions for meaningful
branch names and commit messages, including an imperative subject, a relevant
prefix, and a useful body for non-trivial changes. The logs show examples of
keeping related work separated into commits for the UI workspace, UI pipeline,
workflow removal, and the local test script. The coverage and release work was
committed with the clear subject `test: Expand system coverage and prepare
release`.

This improved productivity because a reviewer could understand the purpose of
each change without reconstructing it from the entire history. It also showed
that an Agent's job is not only to produce files; it can help preserve a clear
engineering record. We would still keep a human review step for the staged
diff and commit message, since a correctly formatted commit can still contain
the wrong change.

## 4. `seedu-git-standard` & `seedu-java-coding-standard`

We also treated the Git and Java coding standards as implementation guardrails.
The Git standard focuses on how a change is presented to the team, while the
Java standard focuses on how the code itself is written. Together, they cover
both the technical change and the collaboration around that change.

The Java standard gives the Agent concrete checks for Java 25 compatibility,
naming, formatting, visibility, control flow, imports, comments, and public
Javadoc. These rules are easy for an Agent to apply consistently across many
files. They improve code quality by encouraging readable names, encapsulation,
predictable formatting, and documentation for public APIs. The Java 25 rule is
especially important here because the release artifact was verified with Java 25 build metadata and the Agent would not casually introduce a newer language feature or silently target an older version.

The standards were most effective when they were combined with project
requirements. They helped keep changes focused and made reviews more
consistent, but they did not replace engineering judgement. We still had to
check that the Agent understood the feature, avoided unrelated refactoring,
and did not change behaviour while applying a convention. In some cases,
following a general rule can create additional review work if the Agent lacks
enough project context.

If we repeated the task, we would combine these standards with an automated
formatting, static-analysis, and staged-diff check. We would also ask the Agent
to report which rules it applied and which checks it could not run. That would
make the result easier to verify without making the instructions much longer.

The main lesson we learned about designing one AI agent for software
engineering is that it needs a clear scope, project-specific sources of truth,
and a way to **show evidence**. And a well-written skill set can guide coding agent
to achieve this.
