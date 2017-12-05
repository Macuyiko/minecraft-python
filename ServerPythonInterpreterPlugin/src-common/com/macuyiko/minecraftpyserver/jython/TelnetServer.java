package com.macuyiko.minecraftpyserver.jython;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class TelnetServer implements Runnable {
	private MinecraftPyServerPlugin plugin;
	private int port;
	private ServerSocket listener;
	protected ExecutorService threadPool;

	public TelnetServer(MinecraftPyServerPlugin caller, int port) {
		this.plugin = caller;
		this.port = port;
		this.threadPool = Executors.newCachedThreadPool();
	}

	public void run() {
		try {
			listener = new ServerSocket(port);
			Socket clientSocket;
			while (true) {
				clientSocket = listener.accept();
				threadPool.execute(new TelnetServerThread(clientSocket, this));
			}
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {
			}
		}

	}

	public ServerSocket getListener() {
		return listener;
	}

	public MinecraftPyServerPlugin getPlugin() {
		return plugin;
	}

}
