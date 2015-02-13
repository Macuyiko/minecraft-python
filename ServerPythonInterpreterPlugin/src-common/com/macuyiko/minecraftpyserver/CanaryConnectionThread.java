package com.macuyiko.minecraftpyserver;

import java.net.Socket;

public class CanaryConnectionThread extends ConnectionThread {
	
	public CanaryConnectionThread(Socket socket, SocketServer socketServer) {
		super(socket, socketServer);
	}

	@Override
	protected boolean parse(final String buffer) {
		return interpreter.runsource(buffer);
	}

}
