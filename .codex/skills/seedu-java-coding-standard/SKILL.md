---
name: seedu-java-coding-standard
description: Apply the SE-EDU basic and intermediate Java coding conventions when creating, editing, or reviewing Java code in this project; do not use it as a replacement for project-specific functional requirements.
---

# Seedu Java Coding Standard

Use this skill for Java implementation and code review in this project. Preserve behavior while bringing code into compliance. When the guide does not cover a topic, follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Java version

- All Java code written or modified for this project must target Java SE 25.
- Use a Java 25 JDK/toolchain for compilation, tests, and API decisions. Configure build tools with Java release/source/target level `25` where those settings exist.
- Do not introduce syntax, APIs, or dependencies requiring a version newer than Java SE 25. Do not silently downgrade to an older Java version; if Java 25 is unavailable or the project configuration conflicts with this requirement, report the issue before proceeding.
- Do not use preview features unless the user explicitly requests them and the build is deliberately configured for Java 25 preview support.

## Naming

- Use lowercase package names. For school projects, start with the group/project name and then logical subpackages; do not use `edu.nus.comp.*`.
- Name classes and enums as nouns in `PascalCase`.
- Name variables and methods in `camelCase`; method names should be verbs.
- Use `SCREAMING_SNAKE_CASE` for constants, with a common prefix for associated constants.
- Test method names may use `featureUnderTest_testScenario_expectedBehavior()`; omit parts when unnecessary.
- Keep acronyms in normal case (`exportHtmlSource`, `openDvdPlayer`), write names/comments in English, and use American spelling without local slang.
- Use descriptive names for large-scope variables; short names such as `i`, `j`, `k`, `c`, and `d` are acceptable for nearby scratch/iterator variables. Use `j`, `k`, etc. only for nested loops.
- Give booleans names that read as predicates, preferably with `is`, `has`, `was`, `can`, or `should`. Boolean setters use `setFound(boolean isFound)` style.
- Use plural names for collections (`points`, `values`).

## Layout and whitespace

- Indent with 4 spaces, never tabs. Keep lines at or below 120 characters; prefer below 110. Wrap long lines with 8 extra spaces relative to the parent line.
- Use K&R braces: the opening brace stays on the declaration/control line.
- Break lines for readability: generally after commas and before operators or operator-like symbols. Keep a method/constructor name attached to its `(`, and prefer higher-level breaks. Ternaries may use either documented two-line form.
- Use the standard method, `if`/`else`, loop, `switch`, and `try`/`catch`/`finally` layouts. For intentional switch fall-through, write an explicit `// Fallthrough` comment. Modern arrow-style switches are also acceptable.
- Surround operators with spaces, put a space after Java reserved words (`if (condition)`), after commas, around binary/ternary colons, and after semicolons in `for` statements.
- Separate logical units in a block with one blank line.

## Packages, imports, and types

- Put every class in a package.
- Keep import ordering consistent across the project, using the team's IDE configuration where available.
- Import classes explicitly; never use wildcard imports. Keep imports minimal and up to date.
- Attach array brackets to the type (`int[] values`), not the variable (`int values[]`).

## Variables, visibility, and control flow

- Initialize variables at declaration when a valid initial value is available, and declare them in the smallest possible scope. Do not use a meaningless placeholder value just to initialize a variable.
- Do not declare class variables `public` unless the class is a behavior-free data class; constants are exempt. Prefer encapsulation through non-public fields and access methods.
- Always wrap loop bodies in braces, even for one statement.
- Put conditional bodies on separate lines and always wrap them in braces, including one-statement conditionals.

## Comments and Javadoc

- Write comments in English, using American spelling and no local slang. Indent comments with the code they describe; trailing comments are allowed when useful.
- Add descriptive header comments (Javadoc) to every public class and public method, except getters/setters, overrides whose inherited Javadoc applies exactly, and test code.
- For Javadoc, put `/**` on its own line, start with a short summary sentence (method summaries should begin with forms such as `Returns`, `Sends`, or `Adds`), align `*`, include a space after each `*`, separate the description from tags with a blank line, and punctuate parameter descriptions. Use `@param`, `@return`, and `@throws` when they add value; use `{@inheritDoc}` for overridden methods that need inherited documentation plus modifications.
- Do not put a blank line between a Javadoc block and the declaration it documents. Simple member documentation may be one line.

## Review checklist

Before finishing Java changes, inspect changed files for Java SE 25 compatibility and toolchain settings, as well as naming, 4-space indentation, line length, brace placement, whitespace, imports, array syntax, variable scope/initialization, field visibility, loop/conditional braces, intentional switch fall-through, and required public API Javadoc. Prefer the smallest formatting-only change that preserves behavior.

Source: [SE-EDU Java coding standard (basic + intermediate)](https://se-education.org/guides/conventions/java/intermediate.html).
