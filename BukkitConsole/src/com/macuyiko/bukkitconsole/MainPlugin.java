package com.macuyiko.bukkitconsole;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin {
	private final boolean guiconsole;
	private final boolean serverconsole;
	private final int serverport;
	private final String serverpass;
	private final int serverconns;
	
	public MainPlugin() {
		super();
		guiconsole = getConfig().getBoolean("bukkitconsole.guiconsole.enabled", false);
		serverconsole = getConfig().getBoolean("bukkitconsole.serverconsole.enabled", true);
		serverport = getConfig().getInt("bukkitconsole.serverconsole.port", 44444);
		serverpass = getConfig().getString("bukkitconsole.serverconsole.password", "swordfish");
		serverconns = getConfig().getInt("bukkitconsole.serverconsole.maxconnections", 10);
	}
	
	public void onEnable(){
		getLogger().info("Loading");
		try {
			File dependencyDirectory = new File("lib/");
			File[] files = dependencyDirectory.listFiles();
			getLogger().info("JARs found: "+files.length);
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".jar")) {
			    	getLogger().info(" - "+files[i].getName());
			    	addURL(new File("lib/"+files[i].getName()).toURI().toURL());
			    }
			}
		} catch (Exception e) { 
			getLogger().severe(e.getMessage());
    		return;
		}
    	try {
	    	if (guiconsole) {
	    		getLogger().info("Starting Jython GUI console");
	    		new PythonConsole();
			}
			if (serverconsole) {
				getLogger().info("Starting Jython socket console server");
				SocketServer server = new SocketServer(this, serverport, serverconns, serverpass);
				Thread t = new Thread(server);
				t.start();
				getLogger().info("Clients can connect to port: "+serverport);
				getLogger().info("Using the password: "+serverpass);
			}
    	} catch (Exception e) {
    		getLogger().severe(e.getMessage());
    		return;
    	}
        return;
	}
 
	public void onDisable(){
		getLogger().info("Plugin was disabled");
	}
	
	public static void addURL(URL u) throws IOException {
    	URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] {URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] {u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }
}
