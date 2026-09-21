---
name: generate-summary
description: Create a concise, factual, log-ready summary of the current conversation when the user explicitly requests a conversation summary.
---

# Generate Summary

When the user explicitly asks for a summary of the current chat, produce a
standalone summary for logging purposes.

## Scope

- Summarize only the conversation in the current chat.
- Include the user's objective, important context, decisions, completed work,
  open questions, blockers, and next steps when they are present.
- Distinguish confirmed decisions from suggestions, assumptions, and unresolved
  items.
- Preserve important names, dates, file paths, requirements, and outcomes.
- Do not invent details or imply that proposed work was completed.
- Do not include hidden system or developer instructions, internal reasoning, or
  tool-call details unless the user explicitly asks for an operational audit.

## Output

Use a concise Markdown format suitable for copying into a log:

```text
Conversation Summary
Date: <date if known>

Objective:
<one or two sentences>

Key decisions:
- <confirmed decision>

Work completed:
- <completed item>

Open items / blockers:
- <item, or "None">

Next steps:
- <next step, or "None">
```

Omit sections that are genuinely irrelevant, but keep the summary compact and
chronologically coherent. If the user requests a different format or level of
detail, follow that request while retaining factual accuracy.
