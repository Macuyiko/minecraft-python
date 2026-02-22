# PyLink Roadmap

## Scope

Migrate the plugin to:

- Python 3 via embedded GraalPy (JVM).
- Script-based lifecycle (`plugins/pylink/scripts/<name>/main.py`), not per-player interpreters.
- `/pylink load|unload|reload <script>` command model.
- Automatic load of all script folders after server startup.
- Per-script isolated runtime/context.
- New Python base layer contract (`pyscript`) replacing `mcapi.py`.
- No telnet/websocket support.

## Target Architecture

## 1. Plugin Bootstrap

- On enable:
  - Initialize `ScriptManager`.
  - Ensure `scripts/` directory exists.
  - Discover folders with `main.py`.
  - Autoload all valid scripts.
  - Register `/pylink` command + tab completion.
- On disable:
  - Unload all scripts cleanly in reverse load order.

## 2. Script Lifecycle

- `load(name)`:
  - Validate folder + `main.py`.
  - Create isolated runtime.
  - Inject host API (`pyscript` facade + helpers).
  - Execute `main.py`.
  - Fire registered `on_load` hooks.
- `unload(name)`:
  - Fire `on_unload` hooks.
  - Unregister commands/listeners/scheduled tasks created by script.
  - Close runtime.
- `reload(name)`:
  - `unload(name)` then `load(name)` with robust error handling.

## 3. Runtime Isolation

- One GraalPy context per script.
- No shared mutable Python globals across scripts.
- Shared Java services are script-scoped via handles.

## 4. Command Surface

- `/pylink load <script>`
- `/pylink unload <script>`
- `/pylink reload <script>`
- Optional future: `/pylink list`

## 5. Tracking/Cleanup Guarantees

- Every script registration returns a handle.
- Plugin tracks all handles per script.
- Unload always attempts full cleanup even if script throws.

## Implementation Phases

- [x] Phase 1: Documentation and API contract freeze (`pyscript`).
- [x] Phase 2: Core Java abstractions (`ScriptManager`, `ScriptInstance`, runtime interfaces).
- [x] Phase 3: GraalPy runtime adapter implementation.
- [x] Phase 4: `/pylink` command and script discovery/autoload.
- [x] Phase 5: Legacy Jython/Py4J removal.
- [ ] Phase 6: Migration examples, tests, and hardening.

## Out of Scope (for now)

- Remote REPL / telnet / websocket control.
- Backward compatibility for per-player interactive REPL sessions.
- Full parity shim for every old `mcapi.py` helper on day one.
