package com.macuyiko.canaryconsole;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import net.canarymod.plugin.Plugin;

public class MainPlugin extends Plugin {
	
	private final boolean guiconsole;
	private final boolean serverconsole;
	private final int serverport;
	private final String serverpass;
	private final int serverconns;
	
	public MainPlugin() {
		super();
		guiconsole = getConfig().getBoolean("canaryconsole.guiconsole.enabled", true);
		serverconsole = getConfig().getBoolean("canaryconsole.serverconsole.enabled", true);
		serverport = getConfig().getInt("canaryconsole.serverconsole.port", 44444);
		serverpass = getConfig().getString("canaryconsole.serverconsole.password", "swordfish");
		serverconns = getConfig().getInt("canaryconsole.serverconsole.maxconnections", 10);
	}
	
    @Override
    public boolean enable() {
    	getLogman().info("Loading");
    	
		try {
			File dependencyDirectory = new File("lib/");
			File[] files = dependencyDirectory.listFiles();
			getLogman().info("JARs found: "+files.length);
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".jar")) {
			    	getLogman().info(" - "+files[i].getName());
			    	addURL(new File("lib/"+files[i].getName()).toURI().toURL());
			    }
			}
		} catch (Exception e) { 
			getLogman().error(e.getMessage());
    		return false;
		}
		
    	getConfig().save();
    	
    	try {
	    	if (guiconsole) {
	    		getLogman().info("Starting Jython GUI console");
				new PythonConsole();
			}
			
			if (serverconsole) {
				getLogman().info("Starting Jython socket console server");
				SocketServer server = new SocketServer(serverport, serverconns, serverpass);
				Thread t = new Thread(server);
				t.start();
				getLogman().info("Clients can connect to port: "+serverport);
				getLogman().info("Using the password: "+serverpass);
			}
    	} catch (Exception e) {
    		getLogman().error(e.getMessage());
    		return false;
    	}
		
        return true;
    }
    
    @Override
    public void disable() {
    	getLogman().info("CanaryConsole: Plugin disabled");
    }
    
    public static void addURL(URL u) throws IOException {
    	// Horrible hack taken from: 
    	// http://stackoverflow.com/questions/60764/how-should-i-load-jars-dynamically-at-runtime
    	// Fix if I figure out if there's a class loader in Canary
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


