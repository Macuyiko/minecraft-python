package com.macuyiko.minecraftpyserver.py4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.macuyiko.minecraftpyserver.MinecraftPyServerUtils;

import py4j.reflection.ClassLoadingStrategy;
import py4j.reflection.ReflectionUtil;

public class PyClassLoaderHack {
	private static ClassLoader includingMeLoader;
	
	// We need to register a separate class loader for Py4J including the current JAR
	// as some bridge classes are located in our own JAR. We can't add this using addURL
	// however as this would lead to internal conflicts in the classloader.
	// Also this helpfun method needs to live in its own class, otherwise we won't be 
	// able to find ClassLoadingStrategy at runtime before we've injected the Py4J JAR
	
	public static void setPy4JClassLoader() {
		ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			File me = new java.io.File(MinecraftPyServerUtils.class.getProtectionDomain()
					  .getCodeSource()
					  .getLocation()
					  .getPath());
			includingMeLoader = new URLClassLoader(new URL[] { me.toURI().toURL() }, threadClassLoader);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			includingMeLoader = threadClassLoader;
		}
		
		ReflectionUtil.setClassLoadingStrategy(new ClassLoadingStrategy() {
			@Override
			public Class<?> classForName(String className) throws ClassNotFoundException {
				return Class.forName(className, true, this.getClassLoader());
			}

			@Override
			public ClassLoader getClassLoader() {
				return includingMeLoader;
			}
		});
	}
}
