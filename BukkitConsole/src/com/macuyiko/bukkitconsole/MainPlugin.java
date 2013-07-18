package com.macuyiko.bukkitconsole;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;

public class MainPlugin extends JavaPlugin {
	public void onEnable(){
		getLogger().info("BukkitConsole: Loading libs");
		try {
			File dependencyDirectory = new File("lib/");
			File[] files = dependencyDirectory.listFiles();
			getLogger().info("BukkitConsole: JARs found: "+files.length);
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".jar")) {
			    	getLogger().info("BukkitConsole:  - "+files[i].getName());
					((PluginClassLoader) this.getClassLoader()).addURL(
						new File("lib/"+files[i].getName()).toURI().toURL());
			    }
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
		
		getLogger().info("BukkitConsole: Creating default config if necessary");
		this.saveDefaultConfig();
		
		if (this.getConfig().getBoolean("bukkitconsole.guiconsole.enabled", false)) {
			getLogger().info("BukkitConsole: Starting Jython GUI console");
			new PythonConsole();
		}
		
		if (this.getConfig().getBoolean("bukkitconsole.serverconsole.enabled", false)) {
			getLogger().info("BukkitConsole: Starting Jython socket console server");
			int port = this.getConfig().getInt("bukkitconsole.serverconsole.port", 44444);
			int maxc = this.getConfig().getInt("bukkitconsole.serverconsole.maxconnections", 10);
			String pass = this.getConfig().getString("bukkitconsole.serverconsole.password", "swordfish");
			SocketServer server = new SocketServer(port, maxc, pass);
			Thread t = new Thread(server);
			t.start();
			getLogger().info("BukkitConsole: Clients can connect to port: "+port);
		}
	}
 
	public void onDisable(){
		getLogger().info("BukkitConsole: Plugin was disabled");
	}
}
