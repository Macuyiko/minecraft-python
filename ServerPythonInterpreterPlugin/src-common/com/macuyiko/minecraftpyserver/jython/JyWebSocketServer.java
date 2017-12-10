package com.macuyiko.minecraftpyserver.jython;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.python.core.PyException;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class JyWebSocketServer extends WebSocketServer {
	private MinecraftPyServerPlugin plugin;
	private Map<WebSocket, JyInterpreter> connections;
	private Map<WebSocket, MyOutputStream> outstreams;
	private Map<WebSocket, String> buffers;
	
	public JyWebSocketServer(MinecraftPyServerPlugin caller, int port) {
		super(new InetSocketAddress(port));
		this.plugin = caller;
		this.connections = new HashMap<WebSocket, JyInterpreter>();
		this.outstreams = new HashMap<WebSocket, MyOutputStream>();
		this.buffers = new HashMap<WebSocket, String>();
	}
	
	public void cleanup() {
		Set<Entry<WebSocket, JyInterpreter>> entries = connections.entrySet();
		for (Entry<WebSocket, JyInterpreter> e : entries){
			if (!connections.get(e.getKey()).isAlive()) {
				plugin.log("Cleaning up an idle connection");
				close(e.getKey());
			}
		}
	}
	
	public void setupInterpreter(WebSocket ws) {
		JyInterpreter interpreter = new JyInterpreter();
		MyOutputStream os = new MyOutputStream(ws);
		interpreter.setOut(os);
		interpreter.setErr(os);
		outstreams.put(ws, os);
		connections.put(ws, interpreter);
		buffers.put(ws, "");
	}
	
	public void close(WebSocket ws) {
		if (connections.containsKey(ws))
			connections.get(ws).cleanAndClose();
		connections.remove(ws);
		buffers.remove(ws);
		ws.close(0);
	}

	@Override
	public void onOpen(WebSocket ws, ClientHandshake chs) {
		plugin.log("New websocket connection");
		cleanup();
		plugin.log("Starting interpreter");
		setupInterpreter(ws);
	}

	@Override
	public void onClose(WebSocket ws, int arg1, String arg2, boolean arg3) {
		close(ws);
	}

	@Override
	public void onMessage(WebSocket ws, final String message) {
		if (message.equals("!exit")) {
			ws.close(CloseFrame.NORMAL);
			return;
		}
		if (message.equals("!restart")) {
			plugin.log("Restarting interpreter");
			setupInterpreter(ws);
			return;
		}	
		
		final MyRunnable runnable = new MyRunnable(ws, message);
		runnable.run();
	}

	@Override
	public void onError(WebSocket ws, Exception exc) {
		close(ws);
	}
	
	public class MyRunnable implements Runnable {
		private WebSocket ws;
		private String message;

		public MyRunnable(final WebSocket ws, final String message) {
			this.ws = ws;
			this.message = message;
		}
		
		@Override
		public void run() {
			final JyInterpreter interpreter = connections.get(ws);
			boolean more = false;
			try {
				if (message.contains("\n")) {
					more = JyParser.parse(interpreter, message, true);
				} else {
					buffers.put(ws, buffers.get(ws)+"\n"+message); 
					more = JyParser.parse(interpreter, buffers.get(ws), false);
				}
			} catch (PyException e) {
				ws.send(e.toString()+"\n");
			}
			if (!more) {
				interpreter.resetbuffer();
				buffers.put(ws, "");
			}
			if (more) ws.send("... ");
			else ws.send(">>> ");
			outstreams.get(ws).flush();
		}
	}

	public class MyOutputStream extends OutputStream {
		WebSocket ws;
		StringBuffer buffer = new StringBuffer("");
		public MyOutputStream(WebSocket ws) {
			this.ws = ws;
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
			this.ws.send(buffer.toString());
			buffer.delete(0, buffer.length());
		}
	}

}
