# PyLink Migration Docs

This folder tracks the migration from the current Jython/Py4J plugin model to a GraalPy-based, script-oriented `PyLink` model.

## Files

- `docs/pylink-roadmap.md`: canonical implementation plan and target architecture.
- `docs/pylink-progress.md`: execution tracker with current status, decision log, and changelog.
- `docs/pyscript-api.md`: draft API contract for Python scripts (`pyscript` base layer).
- `scripts-example/hello/main.py`: minimal script that exercises command, listener, and scheduler APIs.

## Current Focus

1. Validate runtime behavior on live server (especially listener/logging consistency).
2. Stabilize reload/unload cleanup paths under repeated use.
3. Finalize docs after package/class rename to `com.macuyiko.pylink`.
4. Add focused examples and a short migration note for existing users.
