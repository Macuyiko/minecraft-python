# Jython console plugin for Bukkit

By Seppe "Macuyiko" vanden Broucke

`minecraft-bukkit-console` is a Minecraft Bukkit plugin which provides server administrators with a python interpreter console which can be used to administer running servers using the full arsenal provided by the Bukkit API.

The implementation is based on Jython. This has the benefit that the whole Bukkit API can be utilized at runtime, without having to add commands to the Bukkit plugin itself.

On top of that, the console also provides a fun way to learn Python together with Minecraft. Students can see the results of their code immediately reflected in their Minecraft world.

The code is composed out of the following. I assume you have CraftBukkit already running as a server.

* `BukkitConsole`: the Eclipse Java project.
* `python`: contains the Jython Python interpreter console. This uses the excellent work by [Don Coleman](http://don.freeshell.org/jython/), with some minor changes to keep the autocomplete from raising exceptions. **Put this folder in your CraftBukkit installation directory, i.e. next to `plugins`.**
* `lib`: contains the Bukkit API and Jython libraries needed by the plugin. **Put this folder in your CraftBukkit installation directory, i.e. next to `plugins`.**
* `jar`: contains the compiled plugin. **Put `bukkitconsole.jar` in the `plugins` folder of your CraftBukkit installation directory.
