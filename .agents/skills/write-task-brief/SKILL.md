---
name: write-task-brief
description: Create a scoped, verifiable task brief for implementation work in this monorepo.
---

# Write a task brief

1. Read `tasks/README.md` and select the next unused numeric identifier.
2. Copy `tasks/_template.md` to `tasks/NNNN-short-slug.md`.
3. Define the objective, paths in scope, paths reserved for other work, dependencies, acceptance checks, exact verification commands, and rollback/operational concerns.
4. Keep concurrent in-progress task scopes disjoint; sequence shared-file changes.
5. Add the brief to the index and update its status and short work log as implementation proceeds.
6. Use a separate Git worktree for parallel implementation; never stage or commit another worker's changes.
