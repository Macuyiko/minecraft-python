# Minecraft Server Python Interpreter

By: Seppe "Macuyiko" vanden Broucke

**NOTE**: This project is now continued under the new name ["Jycraft"](https://github.com/Jycraft/). This repository is still being updated, but it's goals are less ambitious (i.e. the goal here is to provide a simple Minecraft python interface without advanced features, but which should make it easy to use as a starting point for derived works).

`minecraft-python` is a Spigot plugin which provides server administrators with a Python interpreter console which can be used to administer running servers using the full arsenal provided by the Spigot API. It should be relatively easy to port to other Minecraft servers (Canary support was dropped since the project has ceased activity).

More background information on how this project came to be can be found on [this blog post](http://blog.macuyiko.com/post/2015/rebuilding-our-jython-console-plugin-for-minecraft.html).

You can watch a [Youtube](https://www.youtube.com/watch?v=j4JfwS5hNlw) video showing off some of the possibilities.

The implementation is based on Jython. This has the benefit that the whole Spigot API can be utilized at runtime, i.e. *without* having to register commands to the Spigot plugin itself (though you can define such commands using the interpreter itself at runtime) and without having to type commands through the Minecraft client's chat window, i.e. this deliberately avoids the approach of [ScriptCraft](http://scriptcraftjs.org/) (another great plugin, but we have a different aim here). This makes the plugin itself very simple and always in line with the Spigot API, but also adds some extra complexity, as you might have to peruse the Spigot Javadocs to find your way through all methods.

Other than allowing cool administration possibilities, the console also provides a fun way to learn Python together with Minecraft. Students can see the results of their code immediately reflected in their Minecraft world. The folks over at [Game Start](http://www.gamestartschool.com/) are currently experimenting with using this. If you'd be interesting in collaborate somehow as well, I'm happy to chat.

## Setup

The code is composed out of the following items:

* `ServerPythonInterpreterPlugin`: contains the plugin source code as an Eclipse project. Compatible both with Spigot.
* `ServerEditorWeb`: contains the source code for a simple Java-based Python editor you can use to connect to the server.

Assuming you already have installed Spigot in `SERVER_DIR`, the plugin is installed as follows:

* Place `ServerPythonInterpreterPlugin/python` in `SERVER_DIR` (i.e. you will get a `SERVER_DIR/python` folder). (This step is optional but will allow you to use some shortcut functions.)
* Place `ServerPythonInterpreterPlugin/lib-common` in `SERVER_DIR` (i.e. you will get a `SERVER_DIR/lib-common` folder).

Upon starting, the plugin will create a config file where the following parameters can be set:

* `pythonconsole.serverconsole.password [string]`: interpreter server password (default: swordfish) (not meant to be a strong protection)
* `pythonconsole.serverconsole.telnetport [int]`: port to bind the TCP interpreter server to (default: 44444), set to -1 to disable
* `pythonconsole.serverconsole.websocketport [int]`: port to bind the WebSocket interpreter server to (default: 44445), set to -1 to disable

## Usage

Logging into the interpreter server can be done using telnet, e.g. `telnet 127.0.0.1 44444` you will be prompted for the password and an interpreter will be spawned after successful authentication:

![](https://camo.githubusercontent.com/6fea3b76ec29006ef0e423dc78d3993bc9489797/687474703a2f2f696d6775722e636f6d2f676f4c684733392e706e67)

You can also initiate a RAW connection using PuTTy (make sure to enable "Implicit CR in every LF"):

![](https://camo.githubusercontent.com/6ddb498f728187442e1fca2add801a978d907e75/687474703a2f2f692e696d6775722e636f6d2f316b553276744c2e706e67)

After logging in, it's a good idea to execute `from mcapi_spigot import *` as your first command. This will load in the `mcapi_spigot.py` file included in the distribution which makes a lot of things easier. If you don't want to use the pre-made functions, you can import any Spigot API class as follows: `from org.bukkit import Bukkit` (see the Spigot API docs or `mcapi_spigot.py` for more examples).

It's generally more pleasant to use web based editor to connect to the websocket server, see `ServerEditorWeb` to do so:
    
![](http://i.imgur.com/8ZoH8KG.png)

## Example

Below is a short example of what you can do with the interpreter:

	# Import some modules
	from mcapi_spigot import *
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
	register_hook(BlockDamageEvent, damage_evt)

	# Remove all event hooks -- currently it's not possible to unregister events one by one
	unregister_hooks()

## License

This project is distributed as BSD 3-Clause License software. See `LICENSE.txt` for details.
