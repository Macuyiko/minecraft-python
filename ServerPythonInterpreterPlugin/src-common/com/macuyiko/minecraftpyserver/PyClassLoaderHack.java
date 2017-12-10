package com.macuyiko.minecraftpyserver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import py4j.reflection.ClassLoadingStrategy;
import py4j.reflection.ReflectionUtil;

public class PyClassLoaderHack {
	private static ClassLoader includingMeLoader;
	
	public static void setSelfClassLoader() {
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
