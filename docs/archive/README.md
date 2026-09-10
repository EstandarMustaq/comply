# Archived engineering documents

These documents are retained for historical traceability and audit context. Current product, API, architecture, operations, and roadmap documentation lives under `docs/`, `README.md`, and `ROADMAP.md`.

The archive uses the current product name, **Financial Compliance Intelligent**, in its framing. Historical findings, dates, branch names, API identifiers, and domain terms remain unchanged where changing them would alter the historical record.

Archived material is internal engineering context, not product copy, and must not be exposed through the UI, API, or generated exports.

## Removed ad hoc Python checks

The former root-level `test_auth_flow.py`, `test_full_suite.py`, and `test_backup_and_next_window.py` were removed on 2026-09-10. They embedded environment-specific credentials, fixed localhost assumptions, mutable test data, and obsolete product terminology. They were not part of the supported test runner.

Use the maintained commands instead:

```bash
./scripts/workspace.sh test
./scripts/workspace.sh quality
```

New end-to-end coverage belongs in the component test suites, with secrets supplied by the test environment and fixtures isolated from shared data.
