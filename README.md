# Minecraft Server Python Interpreter

`minecraft-python` is a Spigot plugin providing the ability to control Minecraft using Python. Contrary to other approaches, this project aims to expose the whole Bukkit API to Python, instead of only providing a few commands by hardcoding or wrapping these in a Spigot plugin.

More background information on how this project came to be can be found on [this blog post](http://blog.macuyiko.com/post/2015/rebuilding-our-jython-console-plugin-for-minecraft.html) (a bit outdated at the moment).

You can watch a [Youtube](https://www.youtube.com/watch?v=j4JfwS5hNlw) video showing off some of the possibilities (also a bit outdated by now but gets the idea across).

## Implementation

The implementation is based on Jython. This has the benefit that the complete Python interpreter system runs inside of the JVM, but comes with the drawback that it only supports Python 2.

With the Jython based system, you have the ability to interact with a Jython interpreter through a telnet server, websocket server, and through chat commands (`/py`, `/pyload` and `/pyrestart`). `/py <code>` runs a line of Python code on a Jython intepreter (each player gets their own interpreter). A `.` (dot) at the beginning of the `<code>` line can be used in case indentation with whitespace needs to be provided (the Minecraft server removes whitespace so this is provided as a workaround). `/pyrestart` restarts the Jython interpreter. `/pyload <file>` takes a local Python file (in the running directory or on the Desktop of the server) and executes it in the Jython interpreter.

Alternatively, an HTTP server (port 8080 by default) exposes a web based editor which will connect to the websocket server. This is perhaps the most pleasant way to access the interpreter for now.

Finally, a Telnet client can be used to connect to the telnet server. Note that all interpreters run server-side. No local Python installation is required. A built-in Python module, `mcapi.py`, provides some predefined handy commands. Putting `.py` files in a `python-plugins` directory runs these as "plugins" when starting up the plugin. This interpreter keeps running and can be used to set up global hooks. Other interpreters will be cleaned out after some period of inactivity.

### A Word on Python 3

Jython only supports Python 2 for now, and it seems it'll remain that way for a long while longer. There are various Python 3 <-> JVM interop projects available, though none of which seem to offer the ease-of-use of a full Python on JVM implementation as Jython does.

[Py4j](https://www.py4j.org/) comes close, and an earlier commit did provide a way to interact with Minecraft using this library. However, the Py4J implementation relies heavily on callbacks between Python and a JVM, which are sent over the network. Combining this with lots of thread-juggling and Spigot's internal thread model is daunting to say the least. The implementation works, but is very unstable when trying to perform lots of actions on the Spigot server, so I ultimately removed it from the code base for now. See [this commit](https://github.com/Macuyiko/minecraft-python/tree/168656681a2eb8472b9dbd9b00fea276ac4f6f5d) to get an idea where things ended up -- I might add this back in in a separate branch later on.

At one point in time, I also investigated Lua support, but also put this on the backlog for the time being.

## Comparison

The explicit goal of this project is to allow programming Minecraft using Python and to provide the full Bukkit API in this environment without resorting to manually wrapping these through a Spigot plugin. Other interesting projects in this space are:

* https://github.com/ammaraskar/pyCraft: modern, Python3-compatible, well-documented library for communication with a MineCraft server. This is on the networking level, however, and rather low-level.
* https://github.com/r1chardj0n3s/pycode-minecraft: similar to command blocks, this plugin allows to code scripts on "Python Blocks". Also uses Jython internally.
* http://www.computercraft.info/: an amazing project adding computers and more to Minecraft, provind a coding interface using Lua. This is all in-game, however, comparable to command blocks or `pycode-minecraft`. A fine way to work with computers in Minecraft, though less so to work with Minecraft in computers.
* https://github.com/martinohanlon/mcpi: combines https://github.com/py3minepi/py3minepi and https://github.com/martinohanlon/minecraft-stuff. Exposes only some basic commands by sending them over the wire to a Minecraft: Pi Edition server. 
* https://github.com/zhuowei/RaspberryJuice: a plugin that implements the Minecraft Pi Edition API, so that `mcpi` above can be used together with a normal Minecraft server. https://github.com/wensheng/JuicyRaspberryPie extends this a little bit. https://www.nostarch.com/programwithminecraft uses `RaspberryJuice` + `mcpi` to write its examples. A nice approach, with the downside that many "cool" Spigot commands are not available (fireworks, spawning, explosions, ...).
* http://scriptcraftjs.org/: similar approach, but uses JavaScript and adds more boilerplate code between the JS engine <-> Java interaction. A bit out of date, sadly.

## Setup

As of its latest version, the plugin is installed just like any other Spigot plugin. **You'll need Java 8 at least.**

On boot, `lib-common` and `python` and `lib-http` directories will be created automatically. Config files can be modified to enable/disable servers.

## Example

Below is a short example of what you can do with the interpreter:

	# Import some modules
	from mcapi import *
	from time import sleep
	from random import randint

	MY_NAME = "Macuyiko"

	# Note: all code runs asynchronously by default. If you want to make world edits, Spigot
	# forces you to execute these on a synchronised task. Most of the methods included in mcapi
	# will take care of this automatically

	# Set the time to sundawn
	time(0)

	# Zap the point where I'm looking at
	bolt(lookingat(player(MY_NAME)))

	# A small explosion instead
	explosion(lookingat(player(MY_NAME)), power=2)

	# Generate a tree (only works if there is room)
	tree(lookingat(player(MY_NAME)))

	# Spawn some particles
	particle(lookingat(player(MY_NAME)))

	# Spawn an entity (chicken by default)
	spawn(lookingat(player(MY_NAME)))

	# Fireworks
	fireworks(lookingat(player(MY_NAME)))

	# Let's create an exploding chicken spell

	def exploding_chicken(player_name):
	    yell("Creating an exploding chicken")
	    chicken = spawn(lookingat(player(player_name)))
	    for i in range(5,0,-1):
	        yell("%s second(s) left..." % i)
	        sleep(1)
	    explosion(location(chicken), power=2)
		
		
	# Try it!
	exploding_chicken(MY_NAME)

	# Now let's define a command for this spell
	# Command functions take a special form func(caller, params)

	@asynchronous()
	def cmd_explode_spell(caller, params):
		exploding_chicken(caller.getName())

	# Commands are executed synchronously by Spigot
	# This means that all actions in exploding_chicken would use the state of the 
	# world as it was at the current server tick when executing the command,
	# hence, we use the @asynchronous() decorator to force this function to be 
	# ran asynchronously, only synchronising every time a synchronous command is called

	add_command('chickenspell', cmd_explode_spell)

	# Try typing `/chickenspell` in Minecraft chat window
	# Commands can be unregistered using
	remove_command('chickenspell')

	# Let's register another command to show of the asynchronous workings

	@asynchronous()
	def cmd_growme(caller, params):
	    beginning = lookingat(player(caller.getName()))
	    position = [beginning.x, beginning.y, beginning.z]
	    for i in range(100):
	        setblock(position) # <- will be synchronised
	        position[randint(0,2)] += randint(-1,1)
	        position[1] += +1
	        sleep(0.05)

	add_command('growme', cmd_growme)

	# Now let's register an event
	# These need a func(event) definition

	from org.bukkit.event.block import BlockDamageEvent

	# Almost all events execute synchronised, so again we force them to be asynchronous

	@asynchronous()
	def damage_evt(e):
	    player = e.getPlayer()
	    position = location(player)
	    yell("I'll count to ten, get out!")
	    for i in range(10):
	        yell(str(i))
	        sleep(1.0)
	    explosion(position)

	# A block will explode if the player damages it
	listener = add_event_listener(BlockDamageEvent, damage_evt)

	# Remove specific event hook
	remove_event_listener(listener)

	# Or all hooks
	remove_event_listeners()

Plugins works similarly (place this as a `.py` file in a `python-plugins` directory):

	from mcapi import *
	from time import sleep
	from random import randint

	from org.bukkit.event.player import PlayerJoinEvent

	@asynchronous()
	def join_event(e):
	    player = e.getPlayer()
	    player_location = location(player)
	    player_location.y += 20
	    yell("A chickeny hello to player %s" % (player.getName(),))
	    for i in range(10):
	        spawn(player_location)

	listener = add_event_listener(PlayerJoinEvent, join_event)
	
## License

This project is distributed as BSD 3-Clause License software. See `LICENSE.txt` for details.
