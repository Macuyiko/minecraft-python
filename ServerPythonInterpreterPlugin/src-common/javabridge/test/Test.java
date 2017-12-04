package javabridge.test;

import py4j.ClientServer;

public class Test {
	private PythonCallback callback;
	
	public Test(PythonCallback callback) {
		this.callback = callback;
	}
	
	public void runSynchronous() {
		System.out.println("[runSynchronous] run -> notify");
		this.callback.callback();
	}
	
	public void runAsynchronous() {
		System.out.println("[runAsynchronous] run -> spawn thread");
		ScheduledRunnable runnable = new ScheduledRunnable(callback);
		Thread t = new Thread(runnable);
		t.start();
		System.out.println("[runAsynchronous] return");
	}

	public static void main(String[] args) {
		ClientServer server = new ClientServer(null);
		server.startServer();
	}
	
	
}
