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
		
		getLogger().info("BukkitConsole: Starting Jython console");
		new PythonConsole();
	}
 
	public void onDisable(){
		getLogger().info("BukkitConsole: Plugin was disabled");
	}
}
