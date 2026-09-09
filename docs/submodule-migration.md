# Submodule migration

## Target layout

- `package/compliance-engine` — independently versioned backend repository.
- `app/application` — independently versioned frontend repository.

## Current status

The repository still contains `backend/` and `frontend/`. Real remote URLs are required before adding Git submodules. Placeholder URLs are intentionally not committed as active submodules because Git would make the checkout unusable until those remotes exist.

## Controlled procedure

1. Create or select the two remote repositories.
2. Use history-preserving extraction for each component.
3. Add the extracted repositories as submodules at the target paths.
4. Update Docker, Compose, CI, scripts, environment examples, and documentation paths.
5. Verify a clean clone with `git submodule update --init --recursive`.
6. Remove the old directories only after both submodule builds pass.

## Required inputs before execution

- Backend remote URL.
- Frontend remote URL.
- Default branch and release/tag policy for each component.
- Ownership and access policy for the two repositories.
