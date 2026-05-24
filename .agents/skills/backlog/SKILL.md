---
name: backlog
description: Explicit management of repository-local Markdown backlog tasks. Use only when explicitly invoked as $backlog to create, update, list/search, complete, or explicitly delete completed backlog task files. This skill manages task files only; it is not for implementing tasks, modifying product code, or automatically tracking code work.
---

# Backlog

## Scope

Manage Markdown backlog task files for the current repository only. Do not implement backlog tasks, modify product code, fix bugs, create features, run development work, or infer task completion from code changes.

Respond in the user's language unless they ask otherwise. Write all backlog task file content in English.

## Storage

Use this repository-local directory tree:

```text
Backlog/
  active/
    feature/
    bug/
    improvement/
    tech-debt/
    research/
    documentation/
  done/
    feature/
    bug/
    improvement/
    tech-debt/
    research/
    documentation/
```

If the tree does not exist when a create, update, search, complete, or delete operation is requested, create only the directories needed for that operation, or create the complete tree if simpler. Do not create example task files or modify unrelated project files.

## Task Types

Use only these task types:

- `feature`: New functionality or a substantial new user-visible capability.
- `bug`: Incorrect behavior, regression, crash, broken workflow, or unexpected result.
- `improvement`: Enhancement of existing functionality without introducing a major new capability.
- `tech-debt`: Refactoring, architecture cleanup, dependency updates, maintenance, migration, build cleanup, or internal technical work.
- `research`: Investigation, feasibility analysis, prototype, technical experiment, or decision-making task.
- `documentation`: Standalone documentation, guide, README, integration instruction, or explanatory content task.

Do not add other types automatically. If a request does not clearly fit, ask the user or choose the closest type and mention the classification. Include testing work in the parent feature or bug unless the user explicitly requests a standalone task. Classify routine minor work as `tech-debt`; do not create a `chore` type.

## Identifiers And Filenames

Assign stable IDs by type, using the next available number across both `Backlog/active/` and `Backlog/done/`:

- `FEATURE-001`
- `BUG-001`
- `IMPROVEMENT-001`
- `TECH-DEBT-001`
- `RESEARCH-001`
- `DOCUMENTATION-001`

Use this filename format:

```text
<TASK-ID>-<short-kebab-case-title>.md
```

Examples:

```text
Backlog/active/bug/BUG-001-fix-android-video-orientation.md
Backlog/active/feature/FEATURE-001-add-video-recording-support.md
Backlog/active/research/RESEARCH-001-investigate-encoder-input-surface.md
```

Before creating a task, inspect existing task files in both active and done, determine the next available ID for the selected type, and search for similar active or completed tasks. If a likely duplicate exists, tell the user and ask whether to update or reopen the existing task, or create a separate task.

## Operation Routing

Identify whether the explicit `$backlog` request is to create, update, search/list, complete, or delete. Do not modify files for search/list operations. Do not perform completion or deletion unless the user explicitly requested that operation.

## Create A Task

When the user asks to add a task:

1. Read the user's description.
2. Determine the most appropriate task type.
3. Decide whether the task is clear enough to produce a useful backlog entry.
4. Ask clarification questions only when important missing information or ambiguity would significantly change the task.
5. Create the task file in `Backlog/active/<type>/`.
6. Report the task ID, type, title, and file path.

A task is clear enough when it has a clear goal or problem, enough context to understand the affected system or area, and an expected result or meaningful desired outcome.

Require clarification for a bug when there is not enough information to understand what currently happens, what should happen instead, or where the problem occurs when that is not apparent. Require clarification for a feature when expected user-facing behavior or core scope is unclear. Require clarification for research when there is no clear question to answer or expected conclusion.

## Update A Task

When the user provides additional information for an existing task:

- Find the task by ID when provided.
- If no ID is provided, search for the most likely matching active task.
- If multiple tasks may match, ask the user to select one.
- Update the existing task instead of creating a duplicate.
- Preserve the original task ID.
- Update relevant sections such as requirements, constraints, reproduction steps, acceptance criteria, notes, or open questions.
- Do not change the task type unless the user explicitly requests it or the original type is clearly incorrect. If changing type, explain the change and update the ID and path consistently only after user confirmation.

If the user explicitly chooses to reopen a completed task, move it from `Backlog/done/<type>/` to `Backlog/active/<type>/`, change `Status` to `Active`, remove the `Completed` date, and add a short note that the task was reopened.

## Search Or List Tasks

When the user asks to find or list tasks:

- Search both active and completed tasks unless the user restricts the scope.
- Support queries by task ID, type, keyword, system/module, active status, and completed status.
- Return concise results with task ID, title, type, status, and path.
- Do not modify any files.

## Complete A Task

Mark a task completed only after explicit confirmation from the user. Sufficient confirmations include statements like `BUG-001 is completed`, `FEATURE-002 is done`, `move RESEARCH-001 to done`, or `the video orientation bug is fixed, close the task`.

Do not infer completion merely because code changed, tests passed, a solution was discussed, implementation appears finished, or Codex has just implemented something.

When completing a task:

1. Locate the active task.
2. Change its `Status` section to `Done`.
3. Add a `Completed` date in ISO format: `YYYY-MM-DD`.
4. Move the task file from `Backlog/active/<type>/` to `Backlog/done/<type>/`.
5. Report that the task was moved to done.

Never automatically delete a completed task.

## Delete A Completed Task

Permanent deletion is allowed only after a separate explicit request to delete a specific completed task.

Rules:

- Never delete an active task.
- If the user requests deletion of an active task, explain that active task deletion is not supported. Offer to leave it active, or complete it only if the user explicitly confirms it is complete. Do not introduce a cancelled status.
- Never delete a completed task merely because it has been moved to `done`.
- Before deletion, identify the exact task ID and path being deleted.
- Delete only the requested completed task.

## Task File Format

Use Markdown. Every task must include this shared structure:

```markdown
# <TASK-ID>: <English task title>

## Type
<Feature | Bug | Improvement | Tech Debt | Research | Documentation>

## Status
Active

## Summary
<A concise English summary of the task.>

## Context
<Relevant background, affected system, feature area, platform, or reason this task exists.>

## Requirements
- <Requirement or expected work item>
- <Additional requirement when applicable>

## Acceptance Criteria
- <Observable condition that indicates the task is complete>
- <Additional condition when applicable>

## Constraints
- <Technical, platform, architectural, performance, compatibility, or scope limitation>
- Write `None specified.` when no constraints are known.

## Open Questions
- <Question that is useful to retain but does not prevent task creation>
- Write `None.` when no open questions remain.

## Notes
<Additional information, source of the request, logs, links, or follow-up context. Write `None.` when not applicable.>
```

For `bug` tasks, insert these sections after `Context` and before `Requirements`:

```markdown
## Current Behavior
<What currently happens.>

## Expected Behavior
<What should happen instead.>

## Reproduction Information
<Steps, environment, platform, logs, or other known reproduction details. Write `Not yet provided.` when the bug is clear enough to create but reproduction steps are unavailable.>
```

For `research` tasks, use this structure instead of ordinary `Requirements` and `Acceptance Criteria` when it better fits the request:

```markdown
## Goal
<What must be investigated or established.>

## Questions to Answer
- <Specific investigation question>
- <Additional question when applicable>

## Expected Output
<What artifact or conclusion should be produced: recommendation, prototype, documented constraints, comparison, decision, etc.>

## Acceptance Criteria
- <Conditions that make the research task complete>
```

For `documentation` tasks, add:

```markdown
## Target Audience
<Who should be able to use or understand this documentation.>

## Deliverable
<README section, integration guide, setup instruction, architecture page, user instruction, etc.>
```

Completed tasks must retain the same content and also include:

```markdown
## Status
Done

## Completed
YYYY-MM-DD
```

## Quality Rules

- Write clear technical English in task files.
- Preserve concrete details from the user's input, including platform names, technologies, constraints, error messages, compatibility requirements, and logs.
- Do not invent requirements, platforms, architecture decisions, reproduction steps, completion evidence, or acceptance criteria not supported by the user's input.
- Mark unknown information as `Not yet provided.`, `None specified.`, or an open question.
- Convert informal descriptions into implementation-ready backlog entries without expanding scope.
- Keep acceptance criteria testable and observable.
- Include user-stated constraints explicitly.
- Preserve user-provided log messages verbatim.

## Boundaries

Never implement the task, edit application source code as part of backlog management, mark a task completed without explicit confirmation, permanently delete a task without an explicit deletion request, create duplicate tasks without warning, invent completion evidence, or silently change task type or scope.
