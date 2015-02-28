package com.macuyiko.minecraftpyserver;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.python.util.InteractiveInterpreter;

public class PyWebSocketServer extends WebSocketServer {
	private Object plugin;
	private String password;
	private Map<WebSocket, InteractiveInterpreter> connections;
	private Map<WebSocket, String> buffers;
	
	public PyWebSocketServer (Object caller, int port, String password) {
		super(new InetSocketAddress(port));
		this.plugin = caller;
		this.password = password;
		this.connections = new HashMap<WebSocket, InteractiveInterpreter>();
		this.buffers = new HashMap<WebSocket, String>();
	}
	
	public String getPassword() {
		return password;
	}

	public Object getPlugin() {
		return plugin;
	}

	@Override
	public void onClose(WebSocket ws, int arg1, String arg2, boolean arg3) {
		connections.remove(ws);
		buffers.remove(ws);
	}

	@Override
	public void onError(WebSocket ws, Exception exc) {

	}

	@Override
	public void onMessage(WebSocket ws, final String message) {
		final InteractiveInterpreter interpreter = connections.get(ws);
		if (message.contains("\n")) {
			interpreter.exec(message);
			ws.send(">>> ");
		} else {
			boolean more = false;
			buffers.put(ws, buffers.get(ws)+"\n"+message); 
			more = parse(interpreter, buffers.get(ws));
			if (!more) buffers.put(ws, "");
			if (more) ws.send("... ");
			else ws.send(">>> ");
		}
	}
	
	protected boolean parse(InteractiveInterpreter interpreter, String code) {
		if (ConsolePlugin.isCanary(this.getPlugin()))
			return CanaryParser.parse(interpreter, code);
		else
			return SpigotParser.parse(interpreter, code, this.getPlugin());
	}

	@Override
	public void onOpen(WebSocket ws, ClientHandshake chs) {
		InteractiveInterpreter interpreter = new InteractiveInterpreter(null, ConsolePlugin.getPythonSystemState());
		OutputStream os = new MyOutputStream(ws);
		interpreter.setOut(os);
		interpreter.setErr(os);
		connections.put(ws, interpreter);
		buffers.put(ws, "");
	}
	
	public class MyOutputStream extends OutputStream {
		WebSocket ws;
		public MyOutputStream(WebSocket ws) {
			this.ws = ws;
		}
		@Override
		public void write(int b) {
			int[] bytes = { b };
			write(bytes, 0, bytes.length);
		}
		public void write(int[] bytes, int offset, int length) {
			String s = new String(bytes, offset, length);
			this.ws.send(s);
		}
	}

}
