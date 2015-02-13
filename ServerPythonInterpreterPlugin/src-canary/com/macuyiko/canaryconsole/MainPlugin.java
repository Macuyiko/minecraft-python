package com.macuyiko.canaryconsole;

import java.io.IOException;

import com.macuyiko.minecraftpyserver.ConsolePlugin;
import com.macuyiko.minecraftpyserver.SetupUtils;

import net.canarymod.plugin.Plugin;

public class MainPlugin extends Plugin {
	
	public MainPlugin() {
		super();
	}
	
    @Override
    public boolean enable() {
    	getLogman().info("Loading Python Console");
    	try {
    		int serverport = getConfig().getInt("pythonconsole.serverconsole.port", 44444);
    		String serverpass = getConfig().getString("pythonconsole.serverconsole.password", "swordfish");
    		int serverconns = getConfig().getInt("pythonconsole.serverconsole.maxconnections", 10);
    		getConfig().save();
    		SetupUtils.setup();
    		ConsolePlugin.start(this, serverport, serverpass, serverconns);
		} catch (IOException e) {
			getLogman().error(e.getMessage());
			return false;
		}
    	return true;
    }

	@Override
	public void disable() {}
    
}


