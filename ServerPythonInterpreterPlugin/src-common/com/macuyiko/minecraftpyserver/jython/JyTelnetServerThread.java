package com.macuyiko.minecraftpyserver.jython;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;


public class JyTelnetServerThread implements Runnable {
	private JyTelnetServer server;
	private Socket socket;
	private JyInterpreter interpreter;
	private BufferedReader in;
	private PrintStream out;

	public JyTelnetServerThread(JyTelnetServer socketServer, Socket socket) {
		this.server = socketServer;
		this.socket = socket;
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			this.out = new PrintStream(this.socket.getOutputStream());
		} catch (Exception e) {
			this.server.getPlugin().log("Exception on telnet socket whilst setting up in/out");
			e.printStackTrace();
		}
		setupInterpreter();
		if (socketServer.getPlugin() != null)
			socketServer.getPlugin().log("New telnet connection");
	}

	public void setupInterpreter() {
		if (interpreter != null) {
			interpreter.close();
		}
		int interpretertimeout = server.getPlugin().getConfig().getInt("pythonconsole.disconnecttimeout", 900);
		interpreter = new JyInterpreter(server.getPlugin().getPluginClassLoader(), interpretertimeout);
		this.interpreter.setOut(this.out);
		this.interpreter.setErr(this.out);
	}
	
	public void run() {
		try {
			out.println("Welcome! Don't forget to type '!exit' when you want to logout");
			out.print(">>> ");
			String line = null;
			interpreter.resetbuffer();
			while ((line = in.readLine()) != null) {
				if (line.equals("!exit")) {
					break;
				}
				if (line.equals("!restart")) {
					setupInterpreter();
					out.println();
					out.print(">>> ");
					continue;
				}
				if (!interpreter.isAlive()) {
					out.println("\nInterpreter timeout. Spawn a new one with '!restart'");
					continue;
				}
				boolean more = false;
				more = interpreter.push(line);
				if (!more) {
					out.print(">>> ");
				} else {
					out.print("... ");
				}
			}
			socket.close();
		} catch (IOException ioe) {
			this.server.getPlugin().log("IOException on telnet socket");
		} finally {
			try {
				this.out.close();
				this.in.close();
			} catch (IOException e) {
			}
		}
	}

}
