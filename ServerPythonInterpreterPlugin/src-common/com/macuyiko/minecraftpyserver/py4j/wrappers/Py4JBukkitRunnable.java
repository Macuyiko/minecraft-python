package com.macuyiko.minecraftpyserver.py4j.wrappers;

import org.bukkit.scheduler.BukkitRunnable;

public class Py4JBukkitRunnable extends BukkitRunnable {

	private IPy4JBukkitRunnableFunc func;
	public Object returnValue;
	public boolean done;
	public Exception exception;
	
	public Py4JBukkitRunnable(IPy4JBukkitRunnableFunc func) {
		this.func = func;
	}
	
	@Override
	public void run() {
		try {
			this.returnValue = func.execute();
		} catch (Exception e) {
			exception = e;
		}
		this.done = true;
	}

	public Object getReturnValue() {
		return returnValue;
	}

	public boolean isDone() {
		return done;
	}

	public Exception getException() {
		return exception;
	}

}
