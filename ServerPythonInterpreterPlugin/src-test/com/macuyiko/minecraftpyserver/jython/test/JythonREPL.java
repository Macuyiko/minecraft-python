package com.macuyiko.minecraftpyserver.jython.test;

import java.util.Scanner;

import com.macuyiko.minecraftpyserver.jython.JyInterpreter;

public class JythonREPL {
	public static void main(String[] args) {
		JyInterpreter interpreter = new JyInterpreter();
		Scanner scanner = new Scanner(System.in);
		
		System.out.print(">>> ");
		String line = null;
		interpreter.resetbuffer();
		while ((line = scanner.nextLine()) != null) {
			if (line.equals("!exit")) {
				break;
			}
			if (!interpreter.isAlive()) {
				System.out.println("\nInterpreter timeout");
				break;
			}
			boolean more = false;
			more = interpreter.push(line);
			if (!more) {
				System.out.print(">>> ");
			} else {
				System.out.print("... ");
			}
		}
		
		scanner.close();
		interpreter.close();
	}
}
