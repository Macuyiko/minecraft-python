package com.macuyiko.minecraftpyserver.servers;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.python.core.PyException;

import com.macuyiko.minecraftpyserver.PyInterpreter;
import com.macuyiko.minecraftpyserver.PyPlugin;

public class PyChatServer {
	private PyPlugin plugin;
	private Map<String, PyInterpreter> players;
	private Map<String, MyOutputStream> outstreams;
	private Map<String, String> buffers;
	
	public PyChatServer(PyPlugin caller) {
		this.plugin = caller;
		this.players = new HashMap<String, PyInterpreter>();
		this.outstreams = new HashMap<String, MyOutputStream>();
		this.buffers = new HashMap<String, String>();
	}
		
	public void setupInterpreter(String player) {
		PyInterpreter interpreter = new PyInterpreter();
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
				
		final PyInterpreter interpreter = players.get(player);
		boolean more = false;
		try {
			if (message.contains("\n")) {
				more = plugin.parse(interpreter, message, true);
			} else {
				buffers.put(player, buffers.get(player)+"\n"+message); 
				more = plugin.parse(interpreter, buffers.get(player), false);
			}
		} catch (PyException e) {
			answer(player, e.toString()+"\n");
		}
		outstreams.get(player).flush();
		if (!more) {
			interpreter.resetbuffer();
			buffers.put(player, "");
		}
	}
	
	public void file(String player, File script) {
		if (!players.containsKey(player)) {
			answer(player, "Starting Python... this can take a few seconds\n");
			setupInterpreter(player);
		}
		final PyInterpreter interpreter = players.get(player);
		interpreter.resetbuffer();
		try {
			plugin.parse(interpreter, script);
		} catch (PyException e) {
			answer(player, e.toString()+"\n");
		}
		outstreams.get(player).flush();
		buffers.put(player, "");
	}
	
	public void answer(String player, String message) {
		plugin.send(player, message);
	}

	public class MyOutputStream extends OutputStream {
		String player;
		StringBuffer buffer = new StringBuffer("");
		PyChatServer server;
		public MyOutputStream(PyChatServer server, String player) {
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
