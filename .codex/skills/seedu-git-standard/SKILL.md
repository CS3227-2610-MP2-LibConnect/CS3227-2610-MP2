---
name: seedu-git-standard
description: Apply the SE-EDU Git conventions when creating commits, writing commit messages, reviewing history, or naming branches in this project.
---

# SE-EDU Git Standard

Use this skill for all Git commits and branch naming in this project. Follow the rules below unless the user explicitly requests a different format.

## Commit subject

- Every commit must have a clear, well-written subject.
- Prefer 50 characters or fewer; never exceed 72 characters.
- Use imperative mood (`Add README.md`, not `Added README.md` or `Adding README.md`).
- Capitalize the first letter and do not end with a period.
- Add a relevant `<scope>:` or `<category>:` prefix when useful, such as `Person class:` or `chore:`.

## Commit body

- Non-trivial commits must have a body separated from the subject by one blank line.
- Wrap body lines at 72 characters and use blank lines between paragraphs. Use bullets when they improve clarity.
- Explain what changed and why; do not spend the body explaining implementation mechanics that the diff already shows.
- Make the explanation detailed enough for a reviewer to judge the change without reading the diff. If the message becomes too broad, split the work into finer-grained commits.
- Structure the body around: the present situation, why it needs to change, what is being done, why it is done that way, and other relevant information. Use present tense for the situation and imperative mood for the change.
- Avoid redundant wording such as `currently` or `originally`, and avoid repeating information already present in code comments.

## Branch names

- Use meaningful kebab-case names containing relevant keywords, such as `refactor-ui-tests`.
- For issue-related branches, use `<issueNumber>-<keywords-from-issue-title>`, such as `1234-ui-freeze-error`.

## Before committing

Review the staged diff and commit message against this checklist. Do not create the commit until the subject, body (when required), and branch name comply. Source: [SE-EDU Git conventions](https://se-education.org/guides/conventions/git.html).
