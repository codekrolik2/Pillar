package com.github.bamirov.pulse.interfaces;

import java.util.Collection;

/**
 * Registry for server record cleaners
 * 
 * @author bamirov
 *
 * @param <S> Server ID
 */
public interface ServerPulseRecordCleanerRegistry<S> {
	void addServerRecordCleaner(ServerPulseRecordCleaner<S> serverReferenceRemover);
	boolean removeServerRecordCleaner(ServerPulseRecordCleaner<S> serverReferenceRemover);
	void clearServerRecordCleaners();
	Collection<ServerPulseRecordCleaner<S>> getServerRecordCleaners();
}
