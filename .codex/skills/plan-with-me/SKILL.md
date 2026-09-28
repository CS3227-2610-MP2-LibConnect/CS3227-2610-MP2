---
name: plan-with-me
description: Collaboratively plan application components by comparing at least two implementation approaches, explaining tradeoffs, and requiring confirmation before adopting feedback-driven changes.
---

# Plan With Me

Use this skill when the user asks to design a specific application component, feature, subsystem, or implementation approach. The goal is an informed, collaborative design decision before implementation.

## Initial proposal

First understand the requested component, its current project context, constraints, interfaces, expected behavior, and likely integration points. Inspect relevant files read-only when needed. State important assumptions and identify requirements that are unclear.

Propose at least two meaningfully different implementation approaches. For each approach, include:

- a concise description of the design and how it would fit the application;
- the main implementation shape, affected responsibilities, and important dependencies;
- strengths, such as simplicity, maintainability, performance, testability, extensibility, or compatibility;
- weaknesses, risks, costs, and failure modes;
- circumstances in which the approach is the better choice; and
- any important migration, testing, or operational implications.

Compare the approaches directly and give a reasoned recommendation when the available context supports one. Do not implement code or commit to an approach merely because it is recommended; end by inviting the user to choose, ask for alternatives, or give feedback.

Where possible, propose approaches that are compatible with the existing architecture, technology stack, and project conventions. If a proposed approach would require significant changes to the existing codebase or architecture, clearly explain the implications and tradeoffs.

Additionally, if the user requests a design for a new feature or component, consider whether it can be implemented in a way that is compatible with existing features and components. If not, explain the implications and tradeoffs of introducing a new feature or component that is not compatible with existing ones.

Finally, if there are any existing libraries, frameworks, or tools that could be leveraged to implement the requested component, consider whether they are suitable and explain the implications and tradeoffs of using them versus implementing the component from scratch. For example, ObjectMapper libraries can be used to serialize and deserialize objects to and from JSON instead of having to build a custom JSON parser. In general, you should prioritize using existing libraries, frameworks, or tools over implementing a component from scratch, unless there are compelling reasons to do otherwise.

## Additional alternatives

If the user asks for more alternatives, propose each new approach and evaluate it with the same strengths-and-weaknesses structure. Compare it with the existing options instead of silently replacing them. Keep alternatives realistic for the project's technology, architecture, and requirements.

## Feedback and confirmation

When the user provides feedback that would change a proposed implementation, pause before updating the proposal or making code changes. Assess the suitability of the requested modification and clearly highlight key concerns, tradeoffs, compatibility effects, complexity, testing implications, and any new risks. Then ask whether the user wants you to proceed with that modification.

Do not change the proposal or implementation based on that feedback until the user confirms. The exception is when the user explicitly instructs you to adopt the changes without another confirmation, using clear language such as "just adopt these changes" or "proceed without confirmation." If feedback is only a question or asks for clarification, answer it without treating it as approval.

After confirmation, update the proposal to reflect the accepted changes, explain what changed, and continue the design discussion. If the user rejects the change, retain the prior proposal and explain the resulting decision briefly.

## Implementation

Only implement after the user has selected or explicitly approved an approach and requested implementation. Before editing, summarize the accepted design and scope. Keep changes within that scope and preserve existing behavior outside it.

For Java code, follow the project skill [`.codex/skills/seedu-java-coding-standard/SKILL.md`](../seedu-java-coding-standard/SKILL.md), including its Java SE 25 requirement. Use the project's existing conventions for other languages and tools. Validate the implementation with appropriate tests or checks and report any unresolved concern.
