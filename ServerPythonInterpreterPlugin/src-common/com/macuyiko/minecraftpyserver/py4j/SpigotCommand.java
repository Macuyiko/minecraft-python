package com.macuyiko.minecraftpyserver.py4j;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

public class SpigotCommand extends Command {
	private PyCallback callback;
	private Field _commandMapField;
	private Field _knownCommandsField;
	private CommandMap _commandMap;

	protected SpigotCommand(String name, PyCallback callback) {
		super(name);
		this.callback = callback;
		try {
			_commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			_commandMapField.setAccessible(true);
			_commandMap = (CommandMap) _commandMapField.get(Bukkit.getServer());
			_knownCommandsField = _commandMap.getClass().getDeclaredField("knownCommands");
			_knownCommandsField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	        
	@Override
	public boolean execute(CommandSender caller, String label, String[] parameters) {
		callback.callback(label, parameters);
		return true;
	}
	
	public void remove() {
		try{
			_commandMap.getCommand(this.getName()).unregister(_commandMap);
			@SuppressWarnings("unchecked")
			Map<String,Command> map = (Map<String, Command>) _knownCommandsField.get(_commandMap);
			map.remove(this.getName());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}
}
