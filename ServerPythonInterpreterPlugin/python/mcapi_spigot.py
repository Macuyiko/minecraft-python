from __future__ import absolute_import, division, print_function, unicode_literals
print ('MCAPI activating')

from org.bukkit import *
from org.bukkit.command import Command
from org.bukkit.event import Listener, EventPriority
from org.bukkit.scheduler import BukkitRunnable
from org.bukkit.event import HandlerList

from functools import wraps
from threading import Thread

from random import *
from time import sleep

SERVER 	= Bukkit.getServer()
WORLD 	= SERVER.getWorlds().get(0)
PLUGIN  = SERVER.getPluginManager().getPlugin('MinecraftPyServer')

_commandMapField = SERVER.getClass().getDeclaredField("commandMap")
_commandMapField.setAccessible(True)
_commandMap = _commandMapField.get(SERVER)
_knownCommandsField = _commandMap.getClass().getDeclaredField("knownCommands")
_knownCommandsField.setAccessible(True)
    
TIME_MORNING = 2000
TIME_NOON    = 6000
TIME_EVENING = 14000
TIME_NIGHT   = 18000

class SpigotRunnable(BukkitRunnable):
    def __init__(self, execfunc):
        super(BukkitRunnable, self).__init__()
        self.execfunc = execfunc
    def run(self):
        self.execfunc()
        

def run_local_thread(execfunc):
    Thread(target=execfunc).start()


def run_spigot_thread(execfunc, delay=None):
    spigot_runnable = SpigotRunnable(execfunc)
    if delay is None: spigot_runnable.runTask(PLUGIN)
    else: spigot_runnable.runTaskLater(PLUGIN, delay)


def asynchronous():
    def actual_decorator(f):
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            run_local_thread(g)
        return wrapped_f
    return actual_decorator


def synchronous(delay=None):
    def actual_decorator(f):
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            run_spigot_thread(g, delay)
        return wrapped_f
    return actual_decorator


class SpigotCommand(Command):
    def __init__(self, name, execfunc):
        Command.__init__(self, name)
        self.execfunc = execfunc
    def execute(self, caller, label, parameters):
        self.execfunc(caller, parameters)


def register_command(name, execfunc):
    # execfunc(caller, params)
    _commandMap.register("jycraft", SpigotCommand(name, execfunc))


def unregister_command(name):
    _commandMap.getCommand(name).unregister(_commandMap)
    _knownCommandsField.get(_commandMap).remove(name);
    
    
class EventListener(Listener):
    def __init__(self, func):
        self.func = func
    def execute(self, event):
        self.func(event)


def execute_event_listener(listener, event):
    listener.execute(event)


def register_hook(hookCls, execfunc, priority=EventPriority.NORMAL):
    # execfunc(e)
    listener = EventListener(execfunc)
    SERVER.getPluginManager().registerEvent(hookCls, listener, priority, execute_event_listener, PLUGIN)
    return listener


def unregister_hooks():
    HandlerList.unregisterAll(PLUGIN)


def unregister_hook(listener):
    HandlerListunregisterAll(listener)


def parseargswithpos(args, kwargs, asint=True, ledger={}):
    results = {}
    if kwargs is None: kwargs = {}
    if isinstance(args[0], int) or isinstance(args[0], float):
        base = 3
        tr = [args[0], args[1], args[2]]
    elif 'x' in kwargs and 'y' in kwargs and 'z' in kwargs:
        base = 0
        tr = [kwargs['x'], kwargs['y'], kwargs['z']]
    elif isinstance(args[0], list):
        base = 1
        tr = [args[0][0], args[0][1], args[0][2]]
    elif isinstance(args[0], dict):
        base = 1
        tr = [args[0]['x'], args[0]['y'], args[0]['z']]
    else:
        base = 1
        tr = [args[0].x, args[0].y, args[0].z]
    if asint:
        pos = (int(tr[0]), int(tr[1]), int(tr[2]))
    results['x'] = pos[0]
    results['y'] = pos[1]
    results['z'] = pos[2]
    for k,v in ledger.iteritems():
        results[k] = kwargs.get(v[0], None)
        if results[k] is None:
            if len(args) > base+v[1]:
                results[k] = args[base+v[1]]
            else:
                results[k] = v[2]
    return results


def player(name):
    return SERVER.getPlayer(name)


def player_random():
    pl = SERVER.getOnlinePlayers()
    return choice(pl)


def pos(*args):
    if len(args) == 1:
        return args[0].getLocation()
    return Location(WORLD, *args)


def lookingat(entity):
    return entity.getTargetBlock(None, 100)


def yell(message):
    SERVER.broadcastMessage(message)


@synchronous()
def time(time=None):
    if time is None:
        return WORLD.getTime()
    WORLD.setTime(time)


@synchronous()
def weather(rainsnow=False, thunder=False):
    WORLD.setStorm(rainsnow)
    WORLD.setThundering(thunder)


@synchronous()
def explosion(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={'power':['power', 0, 8]})
    WORLD.createExplosion(r['x'], r['y'], r['z'], r['power'], True)


@synchronous()
def teleport(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={'whom':['whom', 0, 'macuyiko']})
    someone = getplayer(r['whom'])
    someone.teleport(pos(r['x'], r['y'], r['z']))


def getblock(*args, **kwargs):
    r = parseargswithpos(args, kwargs)
    return WORLD.getBlockAt(r['x'], r['y'], r['z'])


@synchronous()
def setblock(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={'type':['type', 0, Material.COBBLESTONE]})
    WORLD.getBlockAt(r['x'], r['y'], r['z']).setType(r['type'])


@synchronous()
def line_x(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Material.COBBLESTONE],
        'size':['size', 1, 4]})
    size = min(r['size'], 12)
    for s in range(size):
        setblock(s + r['x'], r['y'], r['z'], r['type'])


@synchronous()
def line_y(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Material.COBBLESTONE],
        'size':['size', 1, 4]})
    size = min(r['size'], 12)
    for s in range(size):
        setblock(r['x'], s + r['y'], r['z'], r['type'])
        

@synchronous()
def line_z(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Material.COBBLESTONE],
        'size':['size', 1, 4]})
    size = min(r['size'], 12)
    for s in range(size):
        setblock(r['x'], r['y'], s + r['z'], r['type'])
        
        
@synchronous()
def cube(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Material.COBBLESTONE],
        'size':['size', 1, 4]})
    size = min(r['size'], 12)
    for x in range(size):
        for y in range(size):
            for z in range(size):
                setblock(x + r['x'], y + r['y'], z + r['z'], r['type'])


@synchronous()
def bolt(*args, **kwargs):
    r = parseargswithpos(args, kwargs)
    WORLD.strikeLightning(pos(r['x'], r['y'], r['z']))


@synchronous()
def bless(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Effect.COLOURED_DUST],
        'vx':['vx', 1, 1],
        'vy':['vy', 2, 1],
        'vz':['vz', 3, 1],
        'sp':['sp', 4, 100],
        'q':['q', 5, 100],
        'r':['r', 6, 20],
        'block':['block', 7, Material.COBBLESTONE],
        'data':['data', 8, 0]})
    WORLD.spigot().playEffect(pos(r['x'], r['y'], r['z']),
                              r['type'], r['block'].getId(),
                              r['data'], r['vx'], r['vy'], r['vz'],
                              r['sp'], r['q'], r['r'])

