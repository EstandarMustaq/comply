# Claude Code instructions

Read the repository-wide engineering guidance in [`AGENTS.md`](AGENTS.md) before changing code. It is the canonical source for architecture, supported commands, organization-level tenant isolation, API/UI contract coordination, Flyway policy, security, and product-language boundaries.

For work under `apps/ui`, follow the UI-specific instructions in `.github/instructions/ui.instructions.md`. Consult the installed Next.js guides under `apps/ui/node_modules/next/dist/docs/` before changing framework-specific behavior.

Reusable, task-specific workflows are in `.agents/skills/`. Run root package scripts rather than inventing per-app alternatives.
