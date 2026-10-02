# Engineering task briefs

Task briefs capture bounded implementation work that needs explicit ownership, acceptance criteria, or coordination.

## Workflow

1. Assign the next unused numeric ID and create `NNNN-short-slug.md` from `_template.md`.
2. List paths the task may change and paths reserved for other concurrent tasks. In-progress scopes must not overlap.
3. Define acceptance criteria that can be verified with repository commands and record required checks.
4. Use a separate branch/worktree for parallel tasks. Do not stage, commit, or revert changes owned by another contributor.
5. Update the task status and work log as the task progresses.
6. Remove completed task briefs only when their history is no longer useful and the owner agrees.

## Status values

`draft`, `ready`, `in-progress`, `review`, `done`, `blocked`.
