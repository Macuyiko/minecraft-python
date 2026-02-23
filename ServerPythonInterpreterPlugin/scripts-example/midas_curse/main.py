import math
import random

from org.bukkit import Material


SWAPS_CALM = [
    {"a": Material.DIRT, "b": Material.GOLD_ORE, "p": 0.25},
    {"a": Material.COARSE_DIRT, "b": Material.DEEPSLATE_GOLD_ORE, "p": 0.20},
    {"a": Material.SAND, "b": Material.REDSTONE_ORE, "p": 0.20},
    {"a": Material.RED_SAND, "b": Material.DEEPSLATE_REDSTONE_ORE, "p": 0.15},
    {"a": Material.GRAVEL, "b": Material.DIAMOND_ORE, "p": 0.12},
    {"a": Material.OAK_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.SPRUCE_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.BIRCH_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.JUNGLE_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.ACACIA_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.DARK_OAK_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.MANGROVE_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.CHERRY_LOG, "b": Material.EMERALD_BLOCK, "p": 0.10},
    {"a": Material.OAK_PLANKS, "b": Material.EMERALD_BLOCK, "p": 0.08},
    {"a": Material.SPRUCE_PLANKS, "b": Material.EMERALD_BLOCK, "p": 0.08},
    {"a": Material.BIRCH_PLANKS, "b": Material.EMERALD_BLOCK, "p": 0.08},
    {"a": Material.STONE, "b": Material.IRON_ORE, "p": 0.10},
    {"a": Material.COBBLESTONE, "b": Material.LAPIS_ORE, "p": 0.08},
    {"a": Material.ANDESITE, "b": Material.COPPER_ORE, "p": 0.10},
    {"a": Material.DIORITE, "b": Material.COAL_ORE, "p": 0.10},
    {"a": Material.CLAY, "b": Material.AMETHYST_BLOCK, "p": 0.06},
    {"a": Material.MUD, "b": Material.AMETHYST_BLOCK, "p": 0.06},
    {"a": Material.NETHERRACK, "b": Material.NETHER_GOLD_ORE, "p": 0.12},
]

SWAPS_MIDAS = [
    {"a": Material.DIRT, "b": Material.GOLD_BLOCK, "p": 0.85},
    {"a": Material.COARSE_DIRT, "b": Material.GOLD_BLOCK, "p": 0.85},
    {"a": Material.GRASS_BLOCK, "b": Material.GOLD_BLOCK, "p": 0.80},
    {"a": Material.SAND, "b": Material.REDSTONE_BLOCK, "p": 0.80},
    {"a": Material.RED_SAND, "b": Material.REDSTONE_BLOCK, "p": 0.80},
    {"a": Material.GRAVEL, "b": Material.DIAMOND_BLOCK, "p": 0.70},
    {"a": Material.OAK_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.SPRUCE_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.BIRCH_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.JUNGLE_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.ACACIA_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.DARK_OAK_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.MANGROVE_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.CHERRY_LOG, "b": Material.EMERALD_BLOCK, "p": 0.80},
    {"a": Material.OAK_PLANKS, "b": Material.EMERALD_BLOCK, "p": 0.75},
    {"a": Material.SPRUCE_PLANKS, "b": Material.EMERALD_BLOCK, "p": 0.75},
    {"a": Material.BIRCH_PLANKS, "b": Material.EMERALD_BLOCK, "p": 0.75},
    {"a": Material.STONE, "b": Material.GOLD_BLOCK, "p": 0.55},
    {"a": Material.COBBLESTONE, "b": Material.IRON_BLOCK, "p": 0.60},
    {"a": Material.ANDESITE, "b": Material.COPPER_BLOCK, "p": 0.65},
    {"a": Material.NETHERRACK, "b": Material.NETHERITE_BLOCK, "p": 0.25},
    {"a": Material.NETHERRACK, "b": Material.GOLD_BLOCK, "p": 0.65},
]


task = None


def is_player(sender):
    return sender is not None and hasattr(sender, "getLocation") and hasattr(sender, "getWorld")


def to_int(value, default):
    try:
        return int(float(value))
    except Exception:
        return default


def clamp(value, lo, hi):
    return max(lo, min(hi, value))


def is_air_type(material):
    return material in (Material.AIR, Material.CAVE_AIR, Material.VOID_AIR)


def is_liquid(material):
    return material in (Material.WATER, Material.LAVA)


def is_surface_junk(material):
    return material in (
        Material.SNOW,
        Material.SNOW_BLOCK,
        Material.GRASS,
        Material.TALL_GRASS,
        Material.FERN,
        Material.LARGE_FERN,
        Material.SEAGRASS,
        Material.TALL_SEAGRASS,
        Material.DEAD_BUSH,
        Material.DANDELION,
        Material.POPPY,
        Material.BLUE_ORCHID,
        Material.ALLIUM,
        Material.AZURE_BLUET,
        Material.RED_TULIP,
        Material.ORANGE_TULIP,
        Material.WHITE_TULIP,
        Material.PINK_TULIP,
        Material.OXEYE_DAISY,
        Material.CORNFLOWER,
        Material.LILY_OF_THE_VALLEY,
        Material.SUNFLOWER,
        Material.LILAC,
        Material.ROSE_BUSH,
        Material.PEONY,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,
        Material.VINE,
        Material.GLOW_LICHEN,
        Material.LILY_PAD,
        Material.OAK_LEAVES,
        Material.SPRUCE_LEAVES,
        Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES,
        Material.ACACIA_LEAVES,
        Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES,
        Material.CHERRY_LEAVES,
        Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES,
    )


def try_swap_type_with(swaps, current_type):
    for swap in swaps:
        if current_type == swap["a"]:
            return swap["b"] if random.random() < swap["p"] else None
        if current_type == swap["b"]:
            return swap["a"] if random.random() < swap["p"] else None
    return None


def find_ground_y(world, x, z):
    top = world.getHighestBlockAt(x, z)
    if top is None:
        return None

    y = top.getY()
    min_y = world.getMinHeight()
    for i in range(41):
        yy = y - i
        if yy < min_y:
            return None
        block = world.getBlockAt(x, yy, z)
        block_type = block.getType()
        if (not is_air_type(block_type)) and (not is_liquid(block_type)) and (not is_surface_junk(block_type)):
            return yy
    return None


def poor_rich_core(sender, radius, depth, swaps, label):
    global task

    radius = clamp(to_int(radius, 32), 1, 256)
    depth = clamp(to_int(depth, 8), 1, 64)

    if not is_player(sender):
        sender.sendMessage("This command must be run by a player.")
        return

    world = sender.getWorld()
    loc = sender.getLocation()
    cx = int(math.floor(loc.getX()))
    cz = int(math.floor(loc.getZ()))

    cols = []
    r2 = radius * radius
    for dx in range(-radius, radius + 1):
        for dz in range(-radius, radius + 1):
            if (dx * dx + dz * dz) <= r2:
                cols.append((cx + dx, cz + dz))

    if task is not None:
        task.cancel()
        task = None

    state = {"idx": 0, "changed": 0}
    total = len(cols)
    cols_per_tick = 80

    sender.sendMessage(
        f"{label}: scanning {total} columns (r={radius}, depth={depth}), batched..."
    )

    def tick():
        global task
        n = 0
        while n < cols_per_tick and state["idx"] < total:
            x, z = cols[state["idx"]]
            state["idx"] += 1
            n += 1

            ground_y = find_ground_y(world, x, z)
            if ground_y is not None:
                for d in range(depth):
                    y = ground_y - d
                    if y < world.getMinHeight():
                        break
                    block = world.getBlockAt(x, y, z)
                    block_type = block.getType()
                    new_type = try_swap_type_with(swaps, block_type)
                    if new_type is not None:
                        block.setType(new_type)
                        state["changed"] += 1

        if state["idx"] >= total:
            if task is not None:
                task.cancel()
                task = None
            sender.sendMessage(f"{label}: done. changed {state['changed']} blocks.")

    task = scheduler.run_repeating(tick, 0, 1)


def poor_rich_stop(sender):
    global task
    if task is not None:
        task.cancel()
        task = None
        if sender is not None:
            sender.sendMessage("poor_rich_stop: stopped.")
    else:
        if sender is not None:
            sender.sendMessage("poor_rich_stop: nothing running.")


def poor_rich(sender, radius, depth):
    poor_rich_core(sender, radius, depth, SWAPS_CALM, "poor_rich")


def midas_curse(sender, radius, depth):
    radius = 24 if radius is None else radius
    depth = 10 if depth is None else depth
    poor_rich_core(sender, radius, depth, SWAPS_MIDAS, "midas_curse")


def cmd_poor_rich(sender, args):
    radius = args[0] if len(args) > 0 else None
    depth = args[1] if len(args) > 1 else None
    poor_rich(sender, radius, depth)


def cmd_midas_curse(sender, args):
    radius = args[0] if len(args) > 0 else None
    depth = args[1] if len(args) > 1 else None
    midas_curse(sender, radius, depth)


def cmd_poor_rich_stop(sender, args):
    poor_rich_stop(sender)


def on_unload():
    global task
    if task is not None:
        task.cancel()
        task = None


pyscript.register_command(
    name="poor_rich",
    handler=cmd_poor_rich,
    description="Swap poor blocks <-> rich blocks in a radius (batched)",
    usage="/poor_rich [radius=32] [depth=8]",
)

pyscript.register_command(
    name="midas_curse",
    handler=cmd_midas_curse,
    description="Aggressive hilarious Midas curse swaps (batched)",
    usage="/midas_curse [radius=24] [depth=10]",
)

pyscript.register_command(
    name="poor_rich_stop",
    handler=cmd_poor_rich_stop,
    description="Stop a running poor_rich scan",
    usage="/poor_rich_stop",
)

pyscript.on_unload(on_unload)
