package com.macuyiko.minecraftpyserver.jython;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.python.core.PyException;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class JyChatServer {
	private MinecraftPyServerPlugin plugin;
	private Map<String, JyInterpreter> players;
	private Map<String, MyOutputStream> outstreams;
	private Map<String, String> buffers;
	
	public JyChatServer(MinecraftPyServerPlugin caller) {
		this.plugin = caller;
		this.players = new HashMap<String, JyInterpreter>();
		this.outstreams = new HashMap<String, MyOutputStream>();
		this.buffers = new HashMap<String, String>();
	}
		
	public void setupInterpreter(String player) {
		if (players.containsKey(player)) {
			players.get(player).cleanAndClose();
		}
		JyInterpreter interpreter = new JyInterpreter();
		MyOutputStream os = new MyOutputStream(this, player);
		interpreter.setOut(os);
		interpreter.setErr(os);
		outstreams.put(player, os);
		players.put(player, interpreter);
		buffers.put(player, "");
	}

	public void command(String player, String message) {
		if (!players.containsKey(player)) {
			answer(player, "Starting Python... this can take a few seconds\n");
			setupInterpreter(player);
		}
				
		final MyRunnable runnable = new MyRunnable(player, message);
		runnable.run();
	}
	
	public void file(String player, File script) {
		if (!players.containsKey(player)) {
			answer(player, "Starting Python... this can take a few seconds\n");
			setupInterpreter(player);
		}
		
		final MyRunnable runnable = new MyRunnable(player, script);
		runnable.run();
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
	
	public class MyRunnable implements Runnable {
		private String player;
		private String message;
		private File script;

		public MyRunnable(final String player, final String message) {
			this.player = player;
			this.message = message;
		}
		
		public MyRunnable(final String player, final File script) {
			this.player = player;
			this.script = script;
		}

		@Override
		public void run() {
			final JyInterpreter interpreter = players.get(player);
			if (script == null) {
				boolean more = false;
				try {
					if (message.contains("\n")) {
						more = JyParser.parse(interpreter, message, true);
					} else {
						buffers.put(player, buffers.get(player)+"\n"+message); 
						more = JyParser.parse(interpreter, buffers.get(player), false);
					}
				} catch (PyException e) {
					answer(player, e.toString()+"\n");
				}
				outstreams.get(player).flush();
				if (!more) {
					interpreter.resetbuffer();
					buffers.put(player, "");
				} else {
					answer(player, "(...)\n");
				}
			} else {
				interpreter.resetbuffer();
				try {
					JyParser.parse(interpreter, script);
				} catch (PyException e) {
					answer(player, e.toString()+"\n");
				}
				outstreams.get(player).flush();
				buffers.put(player, "");
			}
		}
	}

}
