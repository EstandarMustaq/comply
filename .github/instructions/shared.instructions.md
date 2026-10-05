---
applyTo: "**/*"
---

# Shared repository rules

- Follow root `AGENTS.md` as the canonical engineering guide. Use current root pnpm scripts and `scripts/*.mjs`; do not rely on archived or reference-only setup.
- Treat `Organization` as the isolation boundary for all customer data. Derive tenant context from authentication, fail closed without it, and test cross-organization access.
- Write user-facing text and public documentation in Complyz's clear, direct, confident English voice. Keep technical instructions in concise English. Run `pnpm product-language:check` when changing product copy or application messages.
- Preserve existing worktree changes and consult installed framework documentation for framework-specific behavior.
- Apply `.agents/skills/product-quality/SKILL.md` automatically to product changes; review the full customer workflow and scale checks to risk.
