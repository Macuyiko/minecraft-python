# /bin/python
# -*- coding: utf-8 -*-
# from __future__ import absolute_import, division, print_function, unicode_literals
# Doesn't work with Jython, with not much hope of improving
# See https://bugs.jython.org/issue2007

print ('MCAPI activating')

# Set default encoding to utf-8
import os
if os.name == 'java':
    from org.python.core import codecs
    codecs.setDefaultEncoding('utf-8')

from org.bukkit import Bukkit
from org.bukkit import Location, Color, Effect, Material, Sound, TreeType, Particle, FireworkEffect

from org.bukkit.plugin import EventExecutor
from org.bukkit.entity import EntityType
from org.bukkit.command import Command
from org.bukkit.event import Listener, EventPriority, HandlerList
from org.bukkit.scheduler import BukkitRunnable
from org.bukkit.FireworkEffect import Type as FireworkEffectType

from functools import wraps
from threading import Thread

from random import *
from time import sleep

import sys
import traceback


SERVER = Bukkit.getServer()
WORLD  = SERVER.getWorlds().get(0)
PLUGIN = SERVER.getPluginManager().getPlugin('MinecraftPyServer')

_commandMapField = SERVER.getClass().getDeclaredField("commandMap")
_commandMapField.setAccessible(True)
_commandMap = _commandMapField.get(SERVER)


class SpigotRunnable(BukkitRunnable):
    def __init__(self, execfunc):
        super(BukkitRunnable, self).__init__()
        self.execfunc = execfunc
        self.returnval = None
        self.done = False
    def run(self):
        try:
            self.returnval = self.execfunc()
        except Exception as e:
            print('\n*** An error occurred:\n' + str(e))
            sys.stdout.flush()
        self.done = True

class SpigotCommand(Command):
    def __init__(self, name, execfunc):
        Command.__init__(self, name)
        self.execfunc = execfunc
    def execute(self, caller, label, parameters):
        self.execfunc(caller, parameters)

class EventListener(Listener):
    def __init__(self, func):
        self.func = func
    def execute(self, event):
        self.func(event)

# EventExecutor implementation
class Executor(EventExecutor):
    def execute(self, listener, event):
        listener.execute(event)

class AttrWrapper(object):
    def __init__(self, wrapped):
        self._wrapped = wrapped
        self.delay = None
    def __str__(self):
        return self._wrapped.__str__()
    def __getattr__(self, name):
        f = getattr(self._wrapped, name)
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            d = self.delay
            self.delay = None
            return run_spigot_thread(g, delay=d, wait_for=True)
        return wrapped_f

def run_local_thread(execfunc):
    def wrap_exception(g):
        try:
            g()
        except Exception as e:
            traceback.print_exc(file=sys.stderr)
            sys.stderr.flush()
    Thread(target=lambda: wrap_exception(execfunc)).start()
    sys.stderr.flush()

def run_spigot_thread(execfunc, delay, wait_for):
    spigot_runnable = SpigotRunnable(execfunc)
    if delay is None: spigot_runnable.runTask(PLUGIN)
    else: spigot_runnable.runTaskLater(PLUGIN, delay)
    if wait_for:
        while not spigot_runnable.done:
            sleep(0.1)
        return spigot_runnable.returnval
    return spigot_runnable

def asynchronous():
    def actual_decorator(f):
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            return run_local_thread(g)
        return wrapped_f
    return actual_decorator

def synchronous(delay=None, wait_for=True):
    def actual_decorator(f):
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            return run_spigot_thread(g, delay, wait_for)
        return wrapped_f
    return actual_decorator

def add_command(name, execfunc):
    # execfunc signature: execfunc(caller, params)
    _commandMap.register("jycraft", SpigotCommand(name, execfunc))
    return name

def remove_command(name):
    _commandMap.getCommand(name).unregister(_commandMap)
    _commandMap.getKnownCommands().remove(name)

def add_event_listener(event_type, execfunc, priority=EventPriority.NORMAL):
    # execfunc signature: execfunc(event)
    listener = EventListener(execfunc)
    executor = Executor()
    SERVER.getPluginManager().registerEvent(event_type, listener, priority, executor, PLUGIN)
    return listener

def remove_event_listeners():
    HandlerList.unregisterAll(PLUGIN)

def remove_event_listener(listener):
    HandlerList.unregisterAll(listener)

world = AttrWrapper(WORLD)

# -------------------------
# Built-in helper functions
# -------------------------

def parseargswithpos(args, kwargs=None, asint=True, ledger={}):
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
    for k, v in ledger.iteritems():
        results[k] = kwargs.get(v[0], None)
        if results[k] is None:
            if len(args) > base+v[1]:
                results[k] = args[base+v[1]]
            else:
                results[k] = v[2]
    return results

def player(name=None):
    if name:
        return SERVER.getPlayer(name)
    pl = SERVER.getOnlinePlayers()
    return choice(pl)

def location(*args):
    if len(args) == 0:
        return player().getLocation()
    if len(args) == 1:
        return args[0].getLocation()
    return Location(WORLD, *args)

def lookingat(entity=None, distance=100):
    if not entity:
        entity = player()
    return entity.getTargetBlock(None, distance)

def yell(message):
    SERVER.broadcastMessage(message)

@synchronous()
def time(time=None):
    if time is None:
        return WORLD.getTime()
    WORLD.setTime(time)

@synchronous()
def weather(raining=False, thunder=False):
    WORLD.setStorm(raining)
    WORLD.setThundering(thunder)

@synchronous()
def teleport(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={'whom':['whom', 0, None]})
    if not r['whom']:
        r['whom'] = player().getName()
    someone = player(r['whom'])
    someone.teleport(location(r['x'], r['y'], r['z']))

@synchronous()
def getblock(*args, **kwargs):
    r = parseargswithpos(args, kwargs)
    return WORLD.getBlockAt(r['x'], r['y'], r['z'])

@synchronous()
def setblock(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Material.COBBLESTONE]})
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
    return WORLD.strikeLightning(location(r['x'], r['y'], r['z']))

@synchronous()
def explosion(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={'power':['power', 0, 8]})
    return WORLD.createExplosion(r['x'], r['y'], r['z'], r['power'], True)

@synchronous()
def particle(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Particle.SPELL],
        'count':['count', 1, 100],
        'ox':['ox', 2, 1],
        'oy':['oy', 3, 1],
        'oz':['oz', 4, 1],
        'speed':['speed', 5, 100],
        'data':['data', 6, None]})
    WORLD.spawnParticle(r['type'],
        r['x'], r['y'], r['z'],
        r['count'],
        r['ox'], r['oy'], r['oz'],
        r['speed'], r['data'])

@synchronous(wait_for=True)
def spawn(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, EntityType.CHICKEN]})
    return WORLD.spawnEntity(location(r['x'], r['y'], r['z']), r['type'])

@synchronous()
def effect(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Effect.PORTAL_TRAVEL],
        'data':['data', 1, 0],
        'radius': ['radius', 2, 10]})
    WORLD.playEffect(location(r['x'], r['y'], r['z']), r['type'], r['data'], r['radius'])

@synchronous()
def sound(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, Sound.BLOCK_GLASS_BREAK],
        'volume':['volume', 1, 1.0],
        'pitch':['pitch', 2, 1.0]})
    WORLD.playSound(location(r['x'], r['y'], r['z']), r['type'], r['volume'], r['pitch'])

@synchronous()
def tree(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'type':['type', 0, TreeType.TREE]})
    WORLD.generateTree(location(r['x'], r['y'], r['z']), r['type'])

@synchronous()
def fireworks(*args, **kwargs):
    r = parseargswithpos(args, kwargs, ledger={
        'power':['power', 0, 3],
        'builder':['builder', 1, None]})
    if not r['builder']:
        fwe = FireworkEffect.builder().withTrail().withColor(Color.BLUE, Color.RED).build()
    fw = WORLD.spawnEntity(location(r['x'], r['y'], r['z']), EntityType.FIREWORK)
    fwm = fw.getFireworkMeta()
    fwm.addEffect(fwe)
    fwm.setPower(r['power'])
    fw.setFireworkMeta(fwm)
