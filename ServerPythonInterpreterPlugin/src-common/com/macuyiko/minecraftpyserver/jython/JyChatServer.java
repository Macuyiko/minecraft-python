package com.macuyiko.minecraftpyserver.jython;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class JyChatServer {
	private MinecraftPyServerPlugin plugin;
	private Map<String, JyInterpreter> interpreters;
	private Map<String, Boolean> waiting;
	private Map<String, MyOutputStream> outstreams;
	
	public JyChatServer(MinecraftPyServerPlugin caller) {
		this.plugin = caller;
		this.interpreters = new HashMap<String, JyInterpreter>();
		this.outstreams = new HashMap<String, MyOutputStream>();
		this.waiting = new HashMap<String, Boolean>();
	}
		
	public void setupInterpreter(Player player) {
		setupInterpreter(player.getName());
	}

	public void setupInterpreter(String player) {
		if (interpreters.containsKey(player)) {
			interpreters.get(player).close();
		}
		waiting.put(player, false);
		int interpretertimeout = plugin.getConfig().getInt("pythonconsole.disconnecttimeout", 900);
		JyInterpreter interpreter = new JyInterpreter(interpretertimeout);
		MyOutputStream os = new MyOutputStream(this, player);
		interpreter.setOut(os);
		interpreter.setErr(os);
		outstreams.put(player, os);
		interpreters.put(player, interpreter);
	}

	public void command(Player player, String command) {
		command(player.getName(), command);	
	}

	public void command(String player, String message) {
		if (!interpreters.containsKey(player) || !interpreters.get(player).isAlive()) {
			answer(player, "Starting Python... this can take a few seconds\n");
			setupInterpreter(player);
		}
		
		if (waiting.get(player)) {
			answer(player, "(Still busy with your last command)\n");
			return;
		}
		
		Thread threaded = new Thread() {
			public void run() {
				waiting.put(player, true);
				boolean more = interpreters.get(player).push(message);
				waiting.put(player, false);
				if (more) {
					answer(player, "(More input is expected)\n");
				}
			}
		};
		threaded.start();
	}

	public void file(Player player, File script) {
		file(player.getName(), script);
	}
	
	public void file(String player, File script) {
		if (!interpreters.containsKey(player) || !interpreters.get(player).isAlive()) {
			answer(player, "Starting Python... this can take a few seconds\n");
			setupInterpreter(player);
		}
		
		if (waiting.get(player)) {
			answer(player, "(Still busy with your last command)\n");
			return;
		}
		
		Thread threaded = new Thread() {
			public void run() {
				waiting.put(player, true);
				interpreters.get(player).execfile(script);
				waiting.put(player, false);
			}
		};
		threaded.start();
	}
	
	public void answer(Player player, String message) {
		plugin.send(player, message);
	}
	
	public void answer(String player, String message) {
		plugin.send(player, message);
	}

	public class MyOutputStream extends OutputStream {
		String player;
		StringBuffer buffer = new StringBuffer("");
		JyChatServer server;

		public MyOutputStream(JyChatServer server, String player) {
			this.player = player;
			this.server = server;
		}

		@Override
		public void write(int b) {
			byte[] bytes = { (byte) b };
			write(bytes, 0, bytes.length);
		}

		@Override
		public void write(byte[] bytes, int offset, int length) {
			String s = new String(bytes, offset, length);
			buffer.append(s);
			if (buffer.toString().endsWith("\n"))
				this.flush();
		}

		@Override
		public void flush() {
			String sb = buffer.toString();
			if (!sb.equals(""))
				server.answer(player, buffer.toString());
			buffer.delete(0, buffer.length());
		}
	}

}
