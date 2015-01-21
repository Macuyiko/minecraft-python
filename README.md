# Jython console plugin for Canary (formerly Bukkit)

By Seppe "Macuyiko" vanden Broucke

`MinecraftPythonConsole` is a Minecraft Canary plugin which provides server administrators with a Python interpreter console (either locally though a GUI or remotely with an interpreter server) which can be used to administer running servers using the full arsenal provided by the Canary API.

More information can be found on [this blog post](http://blog.macuyiko.com/post/2015/rebuilding-our-jython-console-plugin-for-minecraft.html).

You can watch a [Youtube](https://www.youtube.com/watch?v=j4JfwS5hNlw) video showing off some of the possibilities.

The implementation is based on Jython. This has the benefit that the whole Canary API can be utilized at runtime, without having to register commands to the Canary plugin itself, i.e. this deliberately avoids the approach of [ScriptCraft](http://scriptcraftjs.org/) (which is another great plugin, but we have a different aim here). This makes the plugin itself very simple and always in line with the Canary API, but also adds some extra complexity, as you might have to peruse the Canary Javadocs to find your way through all methods.

Other than allowing cool administration possibilities, the console also provides a fun way to learn Python together with Minecraft. Students can see the results of their code immediately reflected in their Minecraft world. I'm looking for ways to expand this further, either in the form of a book, video series, otherwise. You can let me know (see "Contact" below) in case you'd be interested in this or want to collaborate somehow. One thing I am definitely not interested in is setting up a server-as-a-service platform. I like that the folks over at [http://learntomod.com](learntomod.com) are helping, but the price point might be a bit cheap for some.

## Setup

The code is composed out of the following items. I assume you have Canary (CanaryMod) already running as a server.

* `BukkitConsole`: this is the legacy project (Eclipse project), originally built on top of Bukkit. The Bukkit project is out, so this project is not worked on anymore.
* `CanaryConsole`: Eclipse project for the plugin. If you just want to get up and running, you don't need this, unless you want to make changes to the source.
* `PlaceInCanaryMod/python`: contains the Jython Python interpreter console. This uses the excellent work by [Don Coleman](http://don.freeshell.org/jython/), with some minor changes to keep the autocomplete from raising exceptions. **Put this folder in your CanaryMod installation directory, i.e. next to `plugins`.**
* `PlaceInCanaryMod/lib`: contains the Canary APIs and Jython libraries needed by the plugin. **Put this folder in your CanaryMod installation directory, i.e. next to `plugins`.** (You might want to check if the libs are still up to date, though.)
* `PlaceInCanaryMod/plugins`: contains the compiled plugin. **Put the jar herein in the `plugins` folder of your CanaryMod installation directory.**

Upon starting, the plugin will create a `CanaryConsole.cfg` file in its `config/CanaryConsole` subdirectory where the following config parameters can be set:

* `canaryconsole.guiconsole.enabled [bool]`: start the GUI interpreter locally (default: true)
* `canaryconsole.serverconsole.enabled [bool]`: enable the remote interpreter server (default: true)
* `canaryconsole.serverconsole.password [string]`: interpreter server password (default: swordfish)
* `canaryconsole.serverconsole.port [int]`: port to bind the interpreter server to (default: 44444)
* `canaryconsole.serverconsole.maxconnections [int]`: maximum number of simultaneous connections (default: 10)

Logging into the interpreter server can be done using telnet, e.g. `telnet 127.0.0.1 44444` you will be prompted for the password and an interpreter will be spawned after succesful authentication.

Both for the GUI based and server based interpreter, it's a good idea to execute `from net.canarymod import Canary` as your first command, al you'll need this for anything else.

