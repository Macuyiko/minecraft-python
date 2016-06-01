package com.macuyiko.minecraftpyserver.servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.macuyiko.minecraftpyserver.PyPlugin;

public class PyTelnetServer implements Runnable {
	private PyPlugin plugin;
	private int port;
	private String password;
	private ServerSocket listener;
	protected ExecutorService threadPool;
	
	public PyTelnetServer (PyPlugin caller, int port, String password) {
		this.plugin = caller;
		this.port = port;
		this.password = password;
		this.threadPool = Executors.newCachedThreadPool();
	}
	
	public void run() {
		try {
			listener = new ServerSocket(port);
			Socket clientSocket ;
			while (true) {
				clientSocket = listener.accept();
				
				threadPool.execute(new PyTelnetServerThread(clientSocket, this));
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

	public PyPlugin getPlugin() {
		return plugin;
	}

}
