package com.github.bamirov.pillar.commandline;

/**
 * Reads String commands from command line and processes them one by one.
 * 
 * @author bamirov
 *
 */
public abstract class CommandLineProcessor extends StringCommandsProcessor {
	public CommandLineProcessor() {
		super(System.in);
	}
}
