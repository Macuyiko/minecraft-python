package com.macuyiko.minecraftpyserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import org.python.util.InteractiveInterpreter;

public abstract class ConnectionThread implements Runnable {
	protected Socket socket;
	protected SocketServer server;
	protected InteractiveInterpreter interpreter;
	protected String line;
	protected String buffer;
	protected PrintStream out;
	protected BufferedReader in;
	
	public ConnectionThread(Socket socket, SocketServer socketServer) {
		this.socket = socket;
		this.server = socketServer;
		this.buffer = "";
		this.interpreter = new InteractiveInterpreter(
				null, 
				ConsolePlugin.getPythonSystemState());
		
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			this.out = new PrintStream(this.socket.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.interpreter.setOut(this.out);
		this.interpreter.setErr(this.out);
	}

	public void run() {
		try {	
			out.println("Python Interpreter Server");
			out.println("-------------------------\n");
			out.print("PASSWORD: ");
			
			line = in.readLine();
			if (!server.getPassword().equals(line)) {
				out.println("Incorrect password: "+line);
				socket.close();
				return;
			} else {
				out.println("Welcome! Don't forget to type 'exit!' when you want to logout");
			}
			
			out.print(">>> ");
			while ((line = in.readLine()) != null && !line.equals("exit!")) {
				if (line.equals("stop!")) {
 					server.getListener().close();
 					socket.close();
 					return;
 				}
				buffer += "\n"+line;
				boolean more = parse(buffer);
				if (more) {
					out.print("... ");
				} else {
					buffer = "";
					out.print(">>> ");
				}
			}			
			out.println("Bye");
			socket.close();
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		} finally {
			try {
				this.out.close();
				this.in.close();
			} catch (IOException e) {}
		}
	}

	protected abstract boolean parse(String buffer);

}
