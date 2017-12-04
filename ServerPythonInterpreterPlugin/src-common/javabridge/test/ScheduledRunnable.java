package javabridge.test;

public class ScheduledRunnable implements Runnable {
	
	private PythonCallback callback;

	public ScheduledRunnable(PythonCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void run() {
		System.out.println("[ScheduledRunnable] run -> notify");
		Thread currentThread = Thread.currentThread();
		System.out.print("[ScheduledRunnable] Thread ID: ");
		System.out.println(currentThread.getId());
		this.callback.callback();
	}
}