package com.macuyiko.minecraftpyserver.jython;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;


public class TelnetServerThread implements Runnable {
	protected Socket socket;
	protected TelnetServer server;
	protected JyInterpreter interpreter;
	protected BufferedReader in;
	protected PrintStream out;

	public TelnetServerThread(Socket socket, TelnetServer socketServer) {
		this.socket = socket;
		this.server = socketServer;
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			this.out = new PrintStream(this.socket.getOutputStream());
		} catch (Exception e) {
			System.err.println("[MinecraftPyServer] Exception on telnet socket whilst setting up in/out");
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
		interpreter = new JyInterpreter();
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
			System.err.println("[MinecraftPyServer] IOException on telnet socket");
		} finally {
			try {
				this.out.close();
				this.in.close();
			} catch (IOException e) {
			}
		}
	}

}
