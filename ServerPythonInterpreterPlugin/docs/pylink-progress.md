# PyLink Progress Tracker

## Status

- Current phase: `Phase 6 - Hardening and Validation`
- Overall progress: `94%`
- Last updated: `2026-02-22`

## Milestones

| Milestone | Status | Notes |
|---|---|---|
| Roadmap established | Done | Scope and phases documented. |
| `pyscript` API draft | Done | Contract drafted and partially implemented in runtime bridge. |
| Java scaffolding | Done | `ScriptManager`, `ScriptInstance`, runtime interfaces in place. |
| GraalPy integration | Done | Runtime adapter active with fallback context builder logic. |
| `/pylink` commands | Done | `load|unload|reload|list` + tab completion implemented. |
| Autoload + unload flow | Done | Startup autoload and shutdown unload wired. |
| Legacy code removal | Done | Jython/Py4J sources, commands, config, jars removed. |
| Package/class rename | Done | Source moved to `com.macuyiko.pylink`, main class is `PyLinkPlugin`. |

## Active Tasks

- [x] Create migration documentation set under `docs/`.
- [x] Define phase-based roadmap.
- [x] Review and lock `pyscript` API contract baseline.
- [x] Add `/pylink` command executor + tab completion.
- [x] Implement `pyscript.register_command`, listener registration, and scheduler bridge.
- [x] Replace placeholder runtime with GraalPy implementation.
- [x] Remove Jython/Py4J source paths and old config/commands.
- [x] Rename plugin identity to `pylink`.
- [x] Rename package/class structure to `com.macuyiko.pylink` / `PyLinkPlugin`.
- [x] Final live-server validation pass for listener/logging edge cases.
- [ ] Final release notes (basically update `README.md`).
- [ ] Add some fun examples (from my previous lualink scripts).
- [ ] Add a util library which can handle batched world updates or other tasks in a friendly way for plugins to use, e.g. spread out a task over x/y positions over multiple ticks (we can do this after converting the examples).

## Decision Log

| Date | Decision | Rationale |
|---|---|---|
| 2026-02-22 | Use embedded GraalPy | Python 3 support while staying in-JVM. |
| 2026-02-22 | Use `/pylink load|unload|reload` | Cleaner lifecycle command model. |
| 2026-02-22 | Script folder model (`plugins/pylink/scripts/<name>/main.py`) | Predictable structure similar to LuaLink. |
| 2026-02-22 | Autoload scripts on startup | Better operator UX and parity with LuaLink behavior. |
| 2026-02-22 | Remove telnet/websocket concepts | Out of scope for new design. |
| 2026-02-22 | Rename plugin identity/package to PyLink | Align code, plugin name, and server data folder naming. |

## Recent Changes

- Replaced legacy Jython/Py4J runtime with GraalPy-based script runtime.
- Implemented script-scoped command/listener/scheduler registration with unload cleanup.
- Added robust `/pylink` error diagnostics and root-cause logging.
- Added command reload hardening and stale command-map cleanup safeguards.
- Switched script logger bridge to Java-backed logging for consistency.
- Added `/pylink list` command.
- Added `pylink.librariesdir` support and automatic `sys.path` setup for:
  - script folder
  - scripts root
  - libraries root
- Removed redundant hardcoded `[PyLink]` log prefixes in runtime logger output.
- Renamed plugin metadata:
  - `plugin.yml` name -> `pylink`
  - main -> `com.macuyiko.pylink.PyLinkPlugin`
- Renamed/moved source tree to `src-common/com/macuyiko/pylink/*`.

## Risks to Track

- GraalPy behavior differences across server JVM distributions.
- Bukkit API thread-safety when scripts use async scheduler variants.
- Command map behavior on different Paper/Spigot versions under repeated reloads.
