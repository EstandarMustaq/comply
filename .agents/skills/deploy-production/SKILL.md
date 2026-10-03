---
name: deploy-production
description: Prepare or operate a Complyz production deployment, release, or rollback through protected workflows and documented recovery controls. Load for production-release requests; require explicit human approval before any deployment, migration, or destructive action.
---

# Production deployment

1. Read the applicable operations runbook and inspect the current release workflow. Production actions require explicit human authorization.
2. Verify the intended revision, CI status, immutable image tag, protected environment, and configured deployment inputs. Do not deploy from an unreviewed or dirty worktree.
3. Confirm a recent verified backup and the recovery procedure before proposing a production change.
4. Present the exact release and migration impact and wait for approval before executing any deployment, database migration, or destructive operation.
5. Use the repository's protected GitHub Actions workflow; do not bypass environment controls or credentials policy.
6. Verify health/readiness after an approved deployment and follow the documented rollback procedure if checks fail.
