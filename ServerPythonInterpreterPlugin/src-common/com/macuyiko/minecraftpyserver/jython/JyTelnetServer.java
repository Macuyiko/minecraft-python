package com.macuyiko.minecraftpyserver.jython;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class JyTelnetServer implements Runnable {
	private MinecraftPyServerPlugin plugin;
	private int port;
	private ServerSocket listener;
	protected ExecutorService threadPool;

	public JyTelnetServer(MinecraftPyServerPlugin caller, int port) {
		this.plugin = caller;
		this.port = port;
		this.threadPool = Executors.newCachedThreadPool();
	}

	public void run() {
		try {
			listener = new ServerSocket(port);
			Socket clientSocket;
			while (!Thread.interrupted()) {
				clientSocket = listener.accept();
				threadPool.execute(new JyTelnetServerThread(this, clientSocket));
			}
		} catch (IOException ioe) {
			plugin.log("IOException on socket listen: " + ioe);
		} finally {
			close();
		}
	}
	
	public void close() {
		try {
			listener.close();
		} catch (IOException e) {
		}
	}

	public ServerSocket getListener() {
		return listener;
	}

	public MinecraftPyServerPlugin getPlugin() {
		return plugin;
	}

}
