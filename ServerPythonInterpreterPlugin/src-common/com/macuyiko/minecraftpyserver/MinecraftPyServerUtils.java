package com.macuyiko.minecraftpyserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class MinecraftPyServerUtils {
	
	private static Logger logger;

	public static void setup(ClassLoader classLoader, Logger logger) {
		MinecraftPyServerUtils.logger = logger;
		
		unpack(".", "lib-common/");
		unpack(".", "python/");

		addURLsToLibraryClassLoader(classLoader, getURLs(new File("lib-common/"), false));
	}
	
	public static URLClassLoader createJythonClassLoader(ClassLoader parent) {
		List<URL> urls = new ArrayList<URL>();
		
		urls.addAll(getURLs(new File("lib-custom/"), true));
		urls.addAll(getURLs(new File("bundler/libraries/"), true));
		urls.addAll(getURLs(new File("libraries/"), true));
		
		URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[] {}), parent);
		return loader;
	}

	public static String os() {
		final String osName = System.getProperty("os.name");
		return osName == null ? "Unknown" : osName;
	}

	public static void unpack(String destDir, String prefix) {
		File df = new File(destDir + java.io.File.separator + prefix);
		df.mkdirs();

		//for (File c : df.listFiles())
		//	if (c.isFile())
		//		c.delete();

		try (JarFile jar = new JarFile(
				MinecraftPyServerUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())) {
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
				JarEntry file = enumEntries.nextElement();
				if (!file.getName().startsWith(prefix))
					continue;
				File f = new File(destDir + java.io.File.separator + file.getName());
				if (logger != null)
					logger.info("Unpacking: " + file.getName());
				f.getParentFile().mkdirs();
				if (f.isDirectory())
					continue;
				if (f.exists())
					f.delete();
				try (InputStream in = new BufferedInputStream(jar.getInputStream(file));
						OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
					byte[] buffer = new byte[2048];
					while (true) {
						int nBytes = in.read(buffer);
						if (nBytes <= 0)
							break;
						out.write(buffer, 0, nBytes);
					}
					out.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			jar.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<URL> getURLs(File directory, boolean recursive) {
		List<URL> urls = new ArrayList<URL>();
		if (!directory.exists() || !directory.isDirectory())
			return urls;

		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (recursive && files[i].isDirectory()) {
				urls.addAll(getURLs(files[i], recursive));
			} else if (files[i].getName().endsWith(".jar")) {
				try {
					urls.add(files[i].toURI().toURL());
				} catch (MalformedURLException e) {}
			}
		}
		
		return urls;
	}
	
	public static void addURLsToLibraryClassLoader(ClassLoader loader, List<URL> urls) {
		for (URL url : urls)
			addURLToLibraryClassLoader(loader, url);
	}

	public static void addURLToLibraryClassLoader(ClassLoader loader, URL u) {
		if (logger != null)
			logger.info("Adding: " + u.toString());

		try {
			Class<?> pluginloaderclass = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
			Field libraryloaderfield = pluginloaderclass.getDeclaredField("libraryLoader");
			libraryloaderfield.setAccessible(true);
			URLClassLoader libraryloader = (URLClassLoader) libraryloaderfield.get(loader);
			URL[] urls = new URL[] {};
			if (libraryloader != null)
				urls = libraryloader.getURLs();
			List<URL> urllist = new ArrayList<URL>(Arrays.asList(urls));
			urllist.add(u);
			libraryloaderfield.set(loader, new URLClassLoader(urllist.toArray(new URL[urllist.size()])));
			return;
		} catch (Throwable t) {
			t.printStackTrace();
		}

		if (logger != null)
			logger.severe("addURL failed! Please file an issue on the GitHub repository");
	}
		
	public static File matchPythonFile(String arg) {
		String homePath = javax.swing.filechooser.FileSystemView.getFileSystemView().getHomeDirectory()
				.getAbsolutePath();
		File asIs = new File(arg);
		File onDesktop = new File(homePath, arg);
		File asIsPy = new File(arg + ".py");
		File onDesktopPy = new File(homePath, arg + ".py");
		if (asIs.exists())
			return asIs;
		if (onDesktop.exists())
			return onDesktop;
		if (asIsPy.exists())
			return asIsPy;
		if (onDesktopPy.exists())
			return onDesktopPy;
		return null;
	}
}
