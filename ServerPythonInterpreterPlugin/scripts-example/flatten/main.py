from org.bukkit import Material


task = None


def is_player(sender):
    return sender is not None and hasattr(sender, "getLocation") and hasattr(sender, "getWorld")


def to_int(value, default_value):
    try:
        return int(float(value))
    except Exception:
        return default_value


def clamp(value, lo, hi):
    return max(lo, min(hi, value))


def flatten_chunk(world, chunk_x, chunk_z, level):
    min_y = world.getMinHeight()
    max_y = world.getMaxHeight()
    base_x = chunk_x << 4
    base_z = chunk_z << 4

    for local_x in range(16):
        for local_z in range(16):
            x = base_x + local_x
            z = base_z + local_z

            top = world.getBlockAt(x, level, z)
            if top.getType() != Material.GRASS_BLOCK:
                top.setType(Material.GRASS_BLOCK)

            for y in range(level + 1, max_y):
                block = world.getBlockAt(x, y, z)
                if block.getType() != Material.AIR:
                    block.setType(Material.AIR)

            for y in range(min_y + 1, level):
                block = world.getBlockAt(x, y, z)
                if block.getType() != Material.DIRT:
                    block.setType(Material.DIRT)


def flatten_command(sender, args):
    global task

    if not is_player(sender):
        sender.sendMessage("This command must be run by a player.")
        return

    if len(args) < 2:
        sender.sendMessage("Usage: /flatten <chunk_radius> <level>")
        return

    world = sender.getWorld()
    location = sender.getLocation()
    chunk = location.getChunk()

    chunk_radius = clamp(to_int(args[0], 1), 0, 32)
    level = clamp(
        to_int(args[1], int(location.getY())),
        world.getMinHeight() + 1,
        world.getMaxHeight() - 1,
    )

    chunk_positions = []
    for dx in range(-chunk_radius, chunk_radius + 1):
        for dz in range(-chunk_radius, chunk_radius + 1):
            chunk_positions.append((chunk.getX() + dx, chunk.getZ() + dz))

    if task is not None:
        task.cancel()
        task = None

    state = {"idx": 0}
    total = len(chunk_positions)

    sender.sendMessage(
        f"Flatten started: {total} chunks, radius={chunk_radius}, level={level}."
    )

    def tick():
        global task
        if state["idx"] >= total:
            if task is not None:
                task.cancel()
                task = None
            sender.sendMessage(f"Flatten complete: {total} chunks at level {level}.")
            return

        chunk_x, chunk_z = chunk_positions[state["idx"]]
        state["idx"] += 1
        flatten_chunk(world, chunk_x, chunk_z, level)

    task = scheduler.run_repeating(tick, 0, 1)


def on_unload():
    global task
    if task is not None:
        task.cancel()
        task = None


pyscript.register_command(
    name="flatten",
    handler=flatten_command,
    description="Flatten chunks around player to a fixed level with grass top.",
    usage="/flatten <chunk_radius> <level>",
)

pyscript.on_unload(on_unload)
