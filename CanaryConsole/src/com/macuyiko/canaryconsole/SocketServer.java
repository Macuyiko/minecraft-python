package com.macuyiko.canaryconsole;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.python.util.PythonInterpreter;

public class SocketServer implements Runnable {
	private int port;
	private int maxConnections;
	private String password;
	private ServerSocket listener;
	
	public SocketServer() {
		this(4444, 10, "");
	}
	
	public SocketServer (int port, int maxConnections, String password) {
		PythonInterpreter.initialize(System.getProperties(), null, new String[] {});
		this.port = port;
		this.maxConnections = maxConnections;
		this.password = password;
	}
	
	public void run() {
		int i = 0;
		try {
			listener = new ServerSocket(port);
			Socket server;
			while ((i++ < maxConnections) || (maxConnections == 0)) {
				server = listener.accept();
				System.out.println("Number of clients: "+i);
				ConnectionThread connection = new ConnectionThread(server, this);
				Thread t = new Thread(connection);
				t.start();
			}
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {}
		}

	}

	public String getPassword() {
		return password;
	}

	public ServerSocket getListener() {
		return listener;
	}

}
