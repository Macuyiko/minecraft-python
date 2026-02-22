def on_load():
    pyscript.logger.info("hello script loaded")


def on_unload():
    pyscript.logger.info("hello script unloading")


def hello_command(sender, args):
    sender.sendMessage("Hello from Python: " + " ".join(args))


def on_join(event):
    player = event.getPlayer()

    def send_welcome():
        player.sendMessage("Welcome, " + player.getName())

    scheduler.run_delayed(send_welcome, 1)


def tick_heartbeat():
    pyscript.logger.debug("heartbeat")


pyscript.on_load(on_load)
pyscript.on_unload(on_unload)

pyscript.register_command(
    name="pyhello",
    handler=hello_command,
    description="Example PyLink command",
    usage="/pyhello [message]",
)

pyscript.register_listener(
    event_class="org.bukkit.event.player.PlayerJoinEvent",
    handler=on_join,
    priority="NORMAL",
    ignore_cancelled=False,
)

scheduler.run_repeating(tick_heartbeat, 20, 20 * 60)
