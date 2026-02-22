# `pyscript` API Draft (v0.1)

This document defines the Python API exposed to each loaded script runtime.

Status: Draft (partially implemented)  
Owner: PyLink migration  
Last updated: 2026-02-22

## Script Layout

- Root folder: `plugins/pylink/scripts/`
- One script per folder: `scripts/<script_name>/`
- Required entrypoint: `scripts/<script_name>/main.py`

## Runtime Model

- Each script is executed in its own isolated GraalPy context.
- A script receives a script-scoped `pyscript` object.
- Registrations created by a script are tracked and auto-cleaned on unload.

## Injected Globals

At `main.py` load time, the host injects:

- `pyscript`: script lifecycle and registration API (required).
- `scheduler`: scheduling helper bound to current script (required).
- `bukkit`: optional convenience bridge for Bukkit access (to be finalized).
- `java`: optional class import/helper bridge (to be finalized).

## Lifecycle API

```python
pyscript.on_load(handler: callable) -> None
pyscript.on_unload(handler: callable) -> None
```

- `on_load` handlers run after `main.py` is successfully executed.
- `on_unload` handlers run before script resources are disposed.
- Multiple handlers are allowed; execution order is registration order.

## Logging

```python
pyscript.logger.info(message: str) -> None
pyscript.logger.warn(message: str) -> None
pyscript.logger.error(message: str) -> None
pyscript.logger.debug(message: str) -> None
```

- Log output is prefixed with script name for traceability.

## Command Registration

```python
handle = pyscript.register_command(
    name="echo",
    handler=callable,           # fn(sender, args)
    aliases=["e"],
    permission="scripts.echo",
    description="Echo text",
    usage="/echo <text>",
    tab_complete=callable       # optional fn(sender, args) -> list[str]
)

handle.unregister()  # optional manual cleanup
```

- Commands are namespaced and tracked by script.
- Unload always unregisters all commands from that script.
- Current implementation note: command callback signature is `handler(sender, args)`.

## Event Registration

```python
handle = pyscript.register_listener(
    event_class="org.bukkit.event.player.PlayerJoinEvent",
    handler=callable,           # fn(event)
    priority="NORMAL",          # LOWEST|LOW|NORMAL|HIGH|HIGHEST|MONITOR
    ignore_cancelled=False
)

handle.unregister()
```

- Event listeners are tracked and removed on script unload.
- Current implementation note: `event_class` expects a fully qualified Java class name string.

## Scheduler API

```python
task = scheduler.run(handler)                           # next tick
task = scheduler.run_delayed(handler, delay_ticks)
task = scheduler.run_repeating(handler, delay_ticks, period_ticks)

task = scheduler.run_async(handler)
task = scheduler.run_delayed_async(handler, delay_ticks)
task = scheduler.run_repeating_async(handler, delay_ticks, period_ticks)

task.cancel()
```

- All tasks are tracked and cancelled on unload.
- Async tasks are allowed but Bukkit/thread-safety remains script author responsibility.
- Current implementation note: handlers are currently invoked with no arguments.

## Script Metadata

```python
pyscript.name: str
pyscript.folder: str
pyscript.data_path: str
```

## Error Handling Contract

- Exceptions during `main.py` execution fail the load operation.
- Exceptions in lifecycle handlers are logged and do not crash the plugin.
- Unload attempts best-effort cleanup even after handler errors.

## Example `main.py`

```python
def _on_load():
    pyscript.logger.info("Loaded.")

def _on_unload():
    pyscript.logger.info("Unloading.")

def echo(sender, args):
    sender.sendMessage(" ".join(args))

pyscript.on_load(_on_load)
pyscript.on_unload(_on_unload)

pyscript.register_command(
    name="echo",
    handler=echo,
    description="Echo text back to sender",
    usage="/echo <text>"
)
```

## Open Questions

- Confirm final shape of `bukkit` and `java` helpers.
- Confirm whether command names must be globally unique or script-namespaced.
- Confirm if `pyscript.yml` metadata file is needed in each script folder.
- Confirm whether reload should preserve script data directory contents by default.
