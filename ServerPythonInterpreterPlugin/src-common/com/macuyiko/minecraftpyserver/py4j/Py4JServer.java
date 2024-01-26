package com.macuyiko.minecraftpyserver.py4j;

import java.net.InetAddress;
import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

import py4j.ClientServer;
import py4j.GatewayServer;

public class Py4JServer implements Runnable {
	
	private MinecraftPyServerPlugin plugin;
	private int port;
	private boolean pinThread = true;
	private ClientServer clientServer;
	private GatewayServer gatewayServer;

	public Py4JServer(MinecraftPyServerPlugin caller, int port) {
		this.plugin = caller;
		this.port = port;
	}

	public void run() {
		InetAddress localhost = InetAddress.getLoopbackAddress();
		if (pinThread) {
			clientServer = new py4j.ClientServer.ClientServerBuilder()
		      .javaPort(this.port)
		      .javaAddress(localhost)
		      .build();
			clientServer.startServer();
		} else {
			gatewayServer = new py4j.GatewayServer.GatewayServerBuilder()
		      .javaPort(this.port)
		      .javaAddress(localhost)
		      .callbackClient(py4j.GatewayServer.DEFAULT_PYTHON_PORT, localhost)
		      .build();
			gatewayServer.start();
		}
	}
	
	public void close() {
		if (clientServer != null) clientServer.shutdown();
		if (gatewayServer != null) gatewayServer.shutdown();
		clientServer = null;
		gatewayServer = null;
	}

	public MinecraftPyServerPlugin getPlugin() {
		return plugin;
	}

}
