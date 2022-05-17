package com.macuyiko.minecraftpyserver.jython;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class JyWebSocketServer extends WebSocketServer {
	private MinecraftPyServerPlugin plugin;
	private Map<WebSocket, JyInterpreter> interpreters;
	private Map<WebSocket, MyOutputStream> outstreams;
	private boolean isClosing = false;
	
	public JyWebSocketServer(MinecraftPyServerPlugin caller, int port) {
		super(new InetSocketAddress(port));
		this.plugin = caller;
		this.interpreters = new HashMap<WebSocket, JyInterpreter>();
		this.outstreams = new HashMap<WebSocket, MyOutputStream>();
		this.isClosing = false;
	}
	
	public void setupInterpreter(WebSocket ws) {
		if (interpreters.containsKey(ws)) {
			interpreters.get(ws).close();
		}
		int interpretertimeout = plugin.getConfig().getInt("pythonconsole.disconnecttimeout", 900);
		JyInterpreter interpreter = new JyInterpreter(plugin.getInterpreterClassLoader(), interpretertimeout);
		MyOutputStream os = new MyOutputStream(ws);
		interpreter.setOut(os);
		interpreter.setErr(os);
		outstreams.put(ws, os);
		interpreters.put(ws, interpreter);
	}
	
	@Override
	public void onStart() {

	}

	@Override
	public void onOpen(WebSocket ws, ClientHandshake chs) {
		plugin.log("New websocket connection");
		setupInterpreter(ws);
		ws.send(">>> ");
	}

	@Override
	public void onClose(WebSocket ws, int arg1, String arg2, boolean arg3) {
		if (isClosing)
			return;
		this.isClosing = true;
		if (interpreters.containsKey(ws))
			interpreters.get(ws).close();
		interpreters.remove(ws);
		outstreams.remove(ws);
	}

	@Override
	public void onMessage(WebSocket ws, final String message) {
		if (message.equals("!exit")) {
			interpreters.get(ws).close();
			ws.close(CloseFrame.NORMAL);
			return;
		}
		if (message.equals("!restart")) {
			setupInterpreter(ws);
			ws.send("\n>>> ");
			return;
		}
		if (!interpreters.get(ws).isAlive()) {
			ws.send("\nInterpreter timeout. Spawn a new one with '!restart'\n");
			ws.send(">>> ");
			return;
		}
		
		boolean more = false;
		more = interpreters.get(ws).push(message);

		outstreams.get(ws).flush();
		
		if (more)
			ws.send("... ");
		else
			ws.send(">>> ");
				
	}

	@Override
	public void onError(WebSocket ws, Exception exc) {
		plugin.log("onError on websocket");
		exc.printStackTrace();
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
			// Right trim lines before sending for better inter-op with 
			// the remote Python interpreter. Not the most elegant solution
			// but simple and makes things on the Python-side way easier
			if (buffer.length() == 0)
				return;
			// -1 is important here
			String[] toSend = buffer.toString().split("\n", -1);
			for (int i = 0; i < toSend.length; i++)
				toSend[i] = toSend[i].replaceAll("\\s+$","");
			this.ws.send(String.join("\n", Arrays.asList(toSend)));
			buffer.delete(0, buffer.length());
		}
	}

}
