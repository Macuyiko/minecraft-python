from net.canarymod import Canary
from net.canarymod import LineTracer
from net.canarymod.api.world.blocks import BlockType
from net.canarymod.api.world.effects import Particle
from net.canarymod.api import GameMode
from net.canarymod.api.world.position import Location
from net.canarymod.api.world.position import Position

from time import *
from random import *
from math import *

SERVER = Canary.getServer()
WORLD = SERVER.getDefaultWorld()
MORNING = 2000
NOON = 6000
EVENING = 14000
NIGHT = 18000


def pos(positionable):
	return positionable.getPosition()

def parseargswithpos(args, kwargs, asint=True, ledger={}):
	results = {}
	if isinstance(args[0], Position):
		base = 1
		if asint:
			pos = (args[0].getBlockX(), args[0].getBlockY(), args[0].getBlockZ())
		else:
			pos = (args[0].getX(), args[0].getY(), args[0].getZ())
	else:
		base = 3
		tr = [args[0], args[1], args[2]]
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

def randomplayer():
	pl = SERVER.getPlayerNameList()
	return player(choice(pl))

def yell(message):
	SERVER.broadcastMessage(message)

def time(time):
	WORLD.setTime(time)
	
def weather(rainsnow, thunder):
	WORLD.setRaining(rainsnow)
	WORLD.setthundering(thunder)

def explosion(*args, **kwargs):
	r = parseargswithpos(args, kwargs, ledger={'power':['power', 0, 8]})
	WORLD.makeExplosion(None, r['x'], r['y'], r['z'], r['power'], True)

def setblock(*args, **kwargs):
	r = parseargswithpos(args, kwargs, ledger={'type':['type', 0, BlockType.Cobble]})
	WORLD.setBlockAt(r['x'], r['y'], r['z'], r['type'])

def cube(*args, **kwargs):
	r = parseargswithpos(args, kwargs, ledger={
		'type':['type', 0, BlockType.Cobble], 
		'size':['size', 1, 4]})
	size = min(r['size'], 12)
	for x in range(size):
		for y in range(size):
			for z in range(size):
				setblock(x + r['x'], y + r['y'], z + r['z'], r['type'])

def bolt(*args):
	r = parseargswithpos(args, kwargs)
	WORLD.makeLightningBolt(r['x'], r['y'], r['z'])

def bless(*args, **kwargs):
	r = parseargswithpos(args, kwargs, ledger={
		'type':['type', 0, Particle.Type.REDSTONE], 
		'vx':['vx', 1, 1],
		'vy':['vy', 2, 1],
		'vz':['vz', 3, 1],
		'sp':['sp', 4, 100],
		'q':['q', 5, 100]})
	par = Particle(r['x'], r['y'], r['z'], r['vx'], r['vy'], r['vz'], r['sp'], r['q'], r['type'])
	WORLD.spawnParticle(par)

def lookingat(player):
	return LineTracer(player).getTargetBlock()

