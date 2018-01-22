package org.pulse;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.pulse.interfaces.ServerPulseRecordCleaner;
import org.pulse.interfaces.ServerPulseRecordCleanerRegistry;

public class ServerPulseRecordCleanerRegistryImpl<S> implements ServerPulseRecordCleanerRegistry<S> {
	protected Set<ServerPulseRecordCleaner<S>> serverHbRecordCleaners;

	public ServerPulseRecordCleanerRegistryImpl() {
		serverHbRecordCleaners = new HashSet<ServerPulseRecordCleaner<S>>();
	}
	
	@Override
	public void addServerRecordCleaner(ServerPulseRecordCleaner<S> serverReferenceRemover) {
		serverHbRecordCleaners.add(serverReferenceRemover);
	}

	@Override
	public boolean removeServerRecordCleaner(ServerPulseRecordCleaner<S> serverReferenceRemover) {
		return serverHbRecordCleaners.remove(serverReferenceRemover);
	}

	@Override
	public void clearServerRecordCleaners() {
		serverHbRecordCleaners.clear();
	}

	@Override
	public Collection<ServerPulseRecordCleaner<S>> getServerRecordCleaners() {
		return serverHbRecordCleaners;
	}
}
