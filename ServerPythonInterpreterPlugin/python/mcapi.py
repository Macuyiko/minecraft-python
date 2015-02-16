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

def explosion(pos, power=8):
	WORLD.makeExplosion(None, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), power, True)

def setblock(pos, ty=BlockType.Cobble):
	WORLD.setBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), ty)

def bolt(pos):
	WORLD.makeLightningBolt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ())

def bless(pos, ty=Particle.Type.REDSTONE, vx=1, vy=1, vz=1, sp=100, q=100):
	par = Particle(pos.x, pos.y, pos.z, vx, vy, vz, sp, q, ty)
	WORLD.spawnParticle(par)

def lookingat(player):
	return LineTracer(player).getTargetBlock()

def pos(positionable):
	return positionable.getPosition()
