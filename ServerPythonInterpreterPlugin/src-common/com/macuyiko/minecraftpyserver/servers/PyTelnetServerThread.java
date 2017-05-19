package com.macuyiko.minecraftpyserver.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import org.python.core.PyException;

import com.macuyiko.minecraftpyserver.PyInterpreter;

public class PyTelnetServerThread implements Runnable {
	protected Socket socket;
	protected PyTelnetServer server;
	protected PyInterpreter interpreter;
	protected String line;
	protected String buffer;
	protected PrintStream out;
	protected BufferedReader in;

	public PyTelnetServerThread(Socket socket, PyTelnetServer socketServer) {
		this.socket = socket;
		this.server = socketServer;
		this.buffer = "";
		this.interpreter = new PyInterpreter();
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			this.out = new PrintStream(this.socket.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.interpreter.setOut(this.out);
		this.interpreter.setErr(this.out);
		socketServer.getPlugin().log("New telnet connection");
	}

	public void run() {
		try {
			out.println("Welcome! Don't forget to type '!exit' when you want to logout");

			out.print(">>> ");
			while ((line = in.readLine()) != null) {
				if (!interpreter.isAlive()) {
					out.print("\nInterpreter timeout");
					break;
				}
				if (line.equals("!exit")) {
					break;
				}
				if (line.equals("!restart")) {
					this.interpreter = new PyInterpreter();
				}
				boolean more = false;
				try {
					if (line.contains("\n")) {
						// As we are using readLine(), this branch will never
						// occur; the telnet interface is thus a pure REPL
						more = server.getPlugin().parse(interpreter, line, true);
					} else {
						buffer += "\n" + line;
						more = server.getPlugin().parse(interpreter, buffer, false);
					}
				} catch (PyException e) {
					out.print(e.toString() + "\n");
				}
				if (!more) {
					buffer = "";
					interpreter.resetbuffer();
					out.print(">>> ");
				} else {
					out.print("... ");
				}
			}
			socket.close();
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		} finally {
			try {
				this.out.close();
				this.in.close();
			} catch (IOException e) {
			}
		}
	}

}
