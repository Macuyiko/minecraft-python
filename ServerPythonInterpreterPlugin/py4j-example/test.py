from mcpyapi import *
from time import sleep

def player(name):
    return SERVER.getPlayer(name)

def yell(message):
    SERVER.broadcastMessage(message)

def time(time=None):
    if time is None:
        return world.getTime()
    world.setTime(time)

print(WORLD)
print(world)

time(12000)

