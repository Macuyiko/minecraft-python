package com.macuyiko.minecraftpyserver.jython;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.SimplePluginManager;

public final class JyCommandListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void event(PlayerCommandPreprocessEvent event) {

		Player sender = event.getPlayer();

		if (event.getMessage().charAt(0) != '/') 
			return;
				
		int index = event.getMessage().indexOf(' ');
		String all = event.getMessage().substring(1, event.getMessage().length());
		String root = event.getMessage().substring(1, index != -1 ? index : event.getMessage().length());

		CommandMap commandMap = getCommandMap();
		
		if (commandMap.getCommand("minecraftpyserver:"+root) == null)
			return;
		
		if (commandMap.dispatch(sender, all))
			event.setCancelled(true);
	}

	private static CommandMap getCommandMap() {
        CommandMap commandMap = null;
        try {
            if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
                Field f = SimplePluginManager.class.getDeclaredField("commandMap");
                f.setAccessible(true);
                commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return commandMap;
    }

}
