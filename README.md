# Jython console plugin for Bukkit

By Seppe "Macuyiko" vanden Broucke

`minecraft-bukkit-console` is a Minecraft Bukkit plugin which provides server administrators with a python interpreter console which can be used to administer running servers using the full arsenal provided by the Bukkit API. You can watch a [Youtube](http://www.youtube.com/watch?v=rI3PfgCSI7Y) video showing off some of the possibilities.

The implementation is based on Jython. This has the benefit that the whole Bukkit API can be utilized at runtime, without having to add commands to the Bukkit plugin itself.

On top of that, the console also provides a fun way to learn Python together with Minecraft. Students can see the results of their code immediately reflected in their Minecraft world.

The code is composed out of the following. I assume you have CraftBukkit already running as a server.

* `BukkitConsole`: the Eclipse Java project. You don't need this if you wish to get up and running as soon as possible.
* `python`: contains the Jython Python interpreter console. This uses the excellent work by [Don Coleman](http://don.freeshell.org/jython/), with some minor changes to keep the autocomplete from raising exceptions. **Put this folder in your CraftBukkit installation directory, i.e. next to `plugins`.**
* `lib`: contains the Bukkit API and Jython libraries needed by the plugin. **Put this folder in your CraftBukkit installation directory, i.e. next to `plugins`.** (You might want to check if the libs are still up to date, though.)
* `jar`: contains the compiled plugin. **Put `bukkitconsole.jar` in the `plugins` folder of your CraftBukkit installation directory.**

In `python`, you'll find the `start.py` script which can be modified to enable one out the following two interaction modes:

1. `import server as plugin` (default): starts a listening server (port and ip can be configured in `server.py`, connect using `server.py IP PORT PASSWORD`. Password is "swordfish", ip 127.0.0.1 and port 33333 by default. Only one user can be connected at ones. This is a bit flaky at the moment, but allows Linux users to play with the plugin as well. I welcome solutions which would allow multiple user logins...

2. `import console as plugin`: starts a GUI console which can be immediately used.

More information can be found on [this blog post](http://blog.macuyiko.com/post/2013/a-bukkit-jython-console-plugin-for-minecraft.html).
