package com.macuyiko.minecraftpyserver;

import java.io.File;

import net.canarymod.plugin.Plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.python.core.PyString;
import org.python.core.PySystemState;

public class ConsolePlugin {
	private ConsolePlugin() {
		
	}
		
	public static void log(Object plugin, String message) {
		if (isCanary(plugin)) {
			((Plugin) plugin).getLogman().info(message);
		} else {
			((JavaPlugin) plugin).getLogger().info(message);
		}
	}
	
	public static void start(Object mainPlugin, int serverport, String serverpass, int serverconns) {
		SocketServer server = new SocketServer(mainPlugin, serverport, serverconns, serverpass);
		Thread t = new Thread(server);
		t.start();
	}

	public static boolean isCanary(Object plugin) {
		return plugin instanceof com.macuyiko.canaryconsole.MainPlugin;
	}
 	
	public static PySystemState getPythonSystemState() {
		PySystemState sys = new PySystemState();
		addPathToPySystemState(sys, "./");
		addPathToPySystemState(sys, "./python/");
		addPathToPySystemState(sys, "./lib-canary/");
		addPathToPySystemState(sys, "./lib-spigot/");
		return sys;
	}
	
	public static void addPathToPySystemState(PySystemState sys, String path) {
		try {
			sys.path.append(new PyString(path));
			File dependencyDirectory = new File(path);
			File[] files = dependencyDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".jar")) {
			    	sys.path.append(new PyString(
			    			new File(path+files[i].getName()).getAbsolutePath()));
			    }
			}
		} catch (Exception e){}
	}
	
}
