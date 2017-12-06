# Minecraft Server Python Interpreter

By: Seppe "Macuyiko" vanden Broucke

**Note:** This project is currently under heavy updates. The README will be expanded soon.

`minecraft-python` is a Spigot plugin providing the ability to control Minecraft using Python. Contrary to other approaches, this project aims to expose the whole Bukkit API to Python, instead of only providing a few commands or wrappers by hardcoding these in a Spigot plugin.

More background information on how this project came to be can be found on [this blog post](http://blog.macuyiko.com/post/2015/rebuilding-our-jython-console-plugin-for-minecraft.html) (a bit outdated at the moment).

You can watch a [Youtube](https://www.youtube.com/watch?v=j4JfwS5hNlw) video showing off some of the possibilities (also a bit outdated by now but gets the idea across).

## Implementation

Currently, two Python "engines" are implemented providing Minecraft <-> Python interoperability.

* One is based on Jython. This has the benefit that the complete Python interpreter system runs inside of the JVM, but comes with the drawback that it only supports Python 2
* A new engine was added recently, based on Py4j. This allows for full Python 3 support

Utilizing the plugin differs somewhat depending on which engine you use:

* With the Jython based system, you have the ability to interact with a Jython interpreter through a telnet server, websocket server, and through chat commands (`/jy`, `/jyload` and `/jyrestart`). An HTTP server exposes a web based editor which'll connect to the websocket server. A telnet command can be used to connect to the telnet server. Note that all intepreters run server-side. No local Python installation is required. A built-in Python module, `mcjyapi`, provides some predefined handy commands. Putting `.py` files in a `python-plugins` directory runs these as "plugins" when starting up the plugin. This interpreter keeps running and can be used to set up global hooks. Other interpreters will be cleaned out after some period of inactivity.
* With the Py4j based system, you'll need to install a Python 3 interpreter manually and use Py4j to connect to the Java gateway server started by the plugin. Chat commands `pyload` and `pyrestart` are available.

A word about these commands `/jy <code>` runs a line of Python code on a Jython intepreter (each player gets their own interpreter). A `.` (dot) prefix can be used in case indentation with whitespace needs to be provided. `jyrestart` restarts the Jython interpreter. `jyload <file>` takes a local Python file (in the running directory or on the Desktop of the server) and executes it in the Jython interpreter. `pyrestart` restarts the Py4j gateway. `pyload <file>` takes a local Python file (in the running directory or on the Desktop of the server) but simply executes it by calling the OS' `python` executable (not as useful), though stdout and stderr are redirected to the chat window. A running `python` executable will be killed off if it runs too long here.

## Comparison

The explicit goal of this project is to allow programming Minecraft using Python and to provide the full Bukkit API in this environment without resorting to manually wrapping these through a Spigot plugin. Other interesting projects are:

* https://github.com/ammaraskar/pyCraft: modern, Python3-compatible, well-documented library for communication with a MineCraft server. This is on the networking level, however.
* https://github.com/r1chardj0n3s/pycode-minecraft: similar to command blocks, this plugin allows to code scripts on "Python Blocks". Also uses Jython internally.
* https://github.com/martinohanlon/mcpi: combines https://github.com/py3minepi/py3minepi and https://github.com/martinohanlon/minecraft-stuff. Exposes only some basic commands by sending them over the wire to Minecraft Pi. 
* https://github.com/zhuowei/RaspberryJuice: a plugin that implements the Minecraft Pi API, so that `mcpi` above can be used together with a normal Minecraft server. https://github.com/wensheng/JuicyRaspberryPie extends this a little bit, https://www.nostarch.com/programwithminecraft uses `RaspberryJuice` + `mcpi` to write its examples
* http://scriptcraftjs.org/: similar approach, but uses JavaScript and adds more boilerplate code between the JS engine <-> Java interaction. A bit outdated, sadly

## Setup

As of its latest version, the plugin is installed just like any other Spigot plugin. On boot, `lib-common` and `python` and `lib-http` directories will be created automatically. Config files can be modified to enable/disable servers.

## Example

Below is a short example of what you can do with the interpreter:

	# Import some modules
	from mcjyapi_spigot import *
	from time import sleep
	from random import randint

	# Set the time to morning
	time(TIME_MORNING)

	# Note: all code runs asynchronously by default. If you want to make world edits, Spigot
	# forces you to execute these on a synchronised task. Most of the methods included in mcapi_spigot
	# will take care of this automatically
	
	MY_NAME = "Macuyiko"
	
	# Zap the point where I'm looking at "bolt" is provided by mcapi_spigot and will be ran on
	# a synchronised thread
	bolt(lookingat(player(MY_NAME)))
	
	def explode_spell(times=10):
		for i in range(times):
			yell("Explosion nr. " + str(i))
			# Explosion is provided by mcapi_spigot and will be ran on a synchronised thread
			explosion(lookingat(player(MY_NAME)), power=2)
			sleep(2)
	# Try it!
	explode_spell()
	
	# Now let's define a command, command functions take a special form func(caller, params)

	@asynchronous()
	def cmd_explode_spell(caller, params):
		explode_spell()
	
	# Commands are executed synchronously by Spigot. This means that all explosions in explode_spell
	# would use the state of the world as it was at the current server tick when executing the command,
	# meaning they would all explode in the same location, hence, we use the @asynchronous() decorator
	# to force this function to be ran asynchronously, only synchronising every time explosion is called

	register_command('boomspell', cmd_explode_spell)
	
	# Try typing `/boomspell` in Minecraft chat window
	# Commands can be unregistered using
	unregister_command('boomspell')

	# Let's register another command to show of the asynchronous workings

	@asynchronous()
	def cmd_growme(caller, params):
	    beginning = lookingat(player(MY_NAME))
	    position = [beginning.x, beginning.y, beginning.z]
	    for i in range(100):
	        setblock(position) # <- will be synchronised
	        position[randint(0,2)] += randint(-1,1)
	        position[1] += +1
	        sleep(0.05)

	register_command('growme', cmd_growme)
	
	# Now let's register an event, these need a func(e) definition
	
	from org.bukkit.event.block import BlockDamageEvent
	
	# Events execute synchronised, so again we force them to be asynchronous
	
	@asynchronous()
	def damage_evt(e):
		player = e.getPlayer()
		position = pos(player)
		yell("I'll count to ten, get out!")
		for i in range(10):
    		yell(str(i))
    		sleep(1.0)
		explosion(position)

	# A block will explode if the player damages it
	listener = register_hook(BlockDamageEvent, damage_evt)

	# Remove event hook
	unregister_hook(listener)

	def cmd_growme(caller, params):
    loc = lookingat(player("Macuyiko")).getLocation()
    world.generateTree(loc, TreeType.BIRCH)

	register_command('growme', cmd_growme)

## License

This project is distributed as BSD 3-Clause License software. See `LICENSE.txt` for details.
