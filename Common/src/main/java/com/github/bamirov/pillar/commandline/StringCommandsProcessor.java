package com.github.bamirov.pillar.commandline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads String commands from stream and processes them one by one.
 * 
 * @author bamirov
 *
 */
public abstract class StringCommandsProcessor {
	protected AtomicBoolean running;
	protected InputStream in;
	
	public StringCommandsProcessor(InputStream in) {
		this.in = in;
	}

	public void readInput() throws Exception {
		running = new AtomicBoolean(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		while (running.get()) {
			try {
				//2.1) Read a command from InputStream 
		        String command = getNextCommand(br);
		        processCommand(command);
			} catch (Exception e) {
				exceptionOccured(e);
			}
		}
	}
	
	protected abstract void processCommand(String command) throws Exception;
	
	protected void exceptionOccured(Exception e) throws Exception {
		e.printStackTrace();
	}
	
	private String getNextCommand(BufferedReader br) throws IOException {
    	return br.readLine();
	}
	
	public void stop() {
		running.set(false);
	}
}
