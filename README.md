# Minecraft Server Python Interpreter

*Or: Jython plugin for Canary/Spigot. Or: Programming with Minecraft.*

By: Seppe "Macuyiko" vanden Broucke

*NOTE*: This project is now continued under the new name 
["Jycraft"](https://github.com/Jycraft/). This repository will not 
receive future updates and is kept here for legacy reasons.


`MinecraftPythonConsole` is a Minecraft Canary (Spigot version available 
too) plugin which provides server administrators with a Python 
interpreter console which can be used to administer running servers 
using the full arsenal provided by the Canary API.

More background information on how this project came to be can be found 
on [this blog post](http://blog.macuyiko.com/post/2015/rebuilding-our-jython-console-plugin-for-minecraft.html).

You can watch a [Youtube](https://www.youtube.com/watch?v=j4JfwS5hNlw) 
video showing off some of the possibilities.

The implementation is based on Jython. This has the benefit that the 
whole Canary API can be utilized at runtime, *without* having to register 
commands to the Canary plugin itself and without having to type commands 
through the Minecraft client's chat window, i.e. this deliberately avoids 
the approach of [ScriptCraft](http://scriptcraftjs.org/) (another great 
plugin, but we have a different aim here). This makes the plugin itself 
very simple and always in line with the Canary API, but also adds some 
extra complexity, as you might have to peruse the Canary Javadocs to find 
your way through all methods.

Other than allowing cool administration possibilities, the console also 
provides a fun way to learn Python together with Minecraft. Students can 
see the results of their code immediately reflected in their Minecraft 
world. The folks over at [Game Start](http://www.gamestartschool.com/) 
are currently experimenting with using this. If you'd be interesting in 
collaborate somehow as well, I'm happy to chat.

## Setup

The code is composed out of the following items:

* `ServerPythonInterpreterPlugin`: contains the plugin source code as an 
Eclipse project. Compatible both with Canary and Spigot.
* `ServerEditorJava`: contains the source code for a simple Java-based 
Python editor you can use to connect to the server.
* `ServerEditorWeb`: contains the source code for a simple Java-based 
Python editor you can use to connect to the server.

(Note: the plugin also used to spawn a local Python interpreter Window, 
but this was removed in a recent update in favor of a decoupled 
client-server architecture.)

Assuming you already have installed Canary in `CANARY_DIR`, the plugin is 
installed as follows:

* Place `ServerPythonInterpreterPlugin/python` in `CANARY_DIR` (i.e. you 
will get a `CANARY_DIR/python` folder). (This step is optional but will 
allow you to use some shortcut functions.)
* Place `ServerPythonInterpreterPlugin/lib-common` in `CANARY_DIR` (i.e. 
you will get a `CANARY_DIR/lib-common` folder).
* Place `ServerPythonInterpreterPlugin/lib-canary` in `CANARY_DIR` (i.e. 
you will get a `CANARY_DIR/lib-canary` folder).

If you're running Spigot instead, replace the last step with:

* Place `ServerPythonInterpreterPlugin/lib-spigot` in `SPIGOT_DIR` (i.e. 
you will get a `SPIGOT_DIR/lib-spigot` folder).

If Canary releases a new CanaryLib package (for new MineCraft versions), 
you can overwrite the JAR in `CANARY_DIR/lib-canary`. Unless significant 
changes happen, this will be enough to keep the plugin working.

Upon starting, the plugin will create a config file where the following 
parameters can be set:

* `canaryconsole.serverconsole.password [string]`: interpreter server 
password (default: swordfish)
* `canaryconsole.serverconsole.telnetport [int]`: port to bind the TCP 
interpreter server to (default: 44444), set to -1 to disable
* `canaryconsole.serverconsole.websocketport [int]`: port to bind the 
WebSocket interpreter server to (default: 44445), set to -1 to disable

On Linux hosts, install can be done like this:

	# Install Canary if you've not done so already
	~$ mkdir canary
	~$ cd canary
	~/canary$ wget http://canarymod.net/download/file/fid/361 -O canary.jar
	# Start for first time and configure (ops, accept eula, ...)
	~/canary$ java -jar canary.jar 
	~/canary$ nano ./config/ops.cfg
	~/canary$ nano eula.txt
	
	# Clone this Git repository
	~/canary$ git clone https://github.com/Macuyiko/MinecraftPythonConsole.git
	~/canary$ ls
	# canary.jar db     eula.txt   MinecraftPythonConsole plugins 
	# worlds     config dbadapters logs                   pluginlangs
	# Copy over python, lib-common, lib-canary folders
	~/canary$ cp -avr ./MinecraftPythonConsole/ServerPythonInterpreterPlugin/python .
	~/canary$ cp -avr ./MinecraftPythonConsole/ServerPythonInterpreterPlugin/lib-common .
	~/canary$ cp -avr ./MinecraftPythonConsole/ServerPythonInterpreterPlugin/lib-canary .
	# Put compiled console.jar plugin binary in plugins (compile this yourself if you prefer)
	~/canary$ cp -avr ./MinecraftPythonConsole/ServerPythonInterpreterPlugin/dist/console.jar ./plugins/console.jar
	# Start canary
	~/canary$ java -jar ./canary.jar

## Usage

Logging into the interpreter server can be done using telnet, e.g. 
`telnet 127.0.0.1 44444` you will be prompted for the password and an 
interpreter will be spawned after successful authentication:

![](https://camo.githubusercontent.com/6fea3b76ec29006ef0e423dc78d3993bc9489797/687474703a2f2f696d6775722e636f6d2f676f4c684733392e706e67)

You can also initiate a RAW connection using PuTTy (make sure to enable 
"Implicit CR in every LF"):

![](https://camo.githubusercontent.com/6ddb498f728187442e1fca2add801a978d907e75/687474703a2f2f692e696d6775722e636f6d2f316b553276744c2e706e67)

After logging in, it's a good idea to execute 
`from net.canarymod import Canary` as your first command, as you'll need 
this for anything else. You can also execute `from mcapi import *` if 
you've added the `python` directory to the Canary installation directory 
as instructed above, which will load a Python file giving you access to 
some simple, pre-defined commands.

You can also use the editor in `ServerEditorJava` to do the same:

![](https://camo.githubusercontent.com/84093a0cfc1102ed283644bcf356c576a7b37422/687474703a2f2f692e696d6775722e636f6d2f436b6a55716e4e2e706e67)

You can also use a web based editor to connect to the websocket server, 
see `ServerEditorWeb` to do so:
    
![](http://i.imgur.com/8ZoH8KG.png)

## License

This project is distributed as BSD 3-Clause License software. See 
`LICENSE.txt` for details.