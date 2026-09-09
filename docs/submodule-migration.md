# Submodule migration

## Target layout

- `package/compliance-engine` — independently versioned backend repository.
- `app/application` — independently versioned frontend repository.

## Current status

The repository still contains `package/compliance-engine/` and `app/application/`. Real remote URLs are required before adding Git submodules. The URLs are now configured in `.gitmodules`; the current environment cannot authenticate to GitHub over SSH, so the extracted commits still need to be pushed to those remotes before a clean external clone can initialize them.

## Controlled procedure

1. Create or select the two remote repositories.
2. Use history-preserving extraction for each component.
3. Add the extracted repositories as submodules at the target paths.
4. Update Docker, Compose, CI, scripts, environment examples, and documentation paths.
5. Verify a clean clone with `git submodule update --init --recursive

The CI checkout must use recursive submodules.`.
6. Remove the old directories only after both submodule builds pass.

## Required inputs before execution

- Backend remote URL.
- Frontend remote URL.
- Default branch and release/tag policy for each component.
- Ownership and access policy for the two repositories.

## Local extraction commits

- Compliance engine history: `df31ae7e5614b396b8b49a6b3c0d9c9e211d81d9`.
- Application history: `e205dc667c9e2ee4fd316d76f1096d1cedc7e2a4`.

Push these histories to the default branches of the two component repositories before publishing the parent workspace commit.
