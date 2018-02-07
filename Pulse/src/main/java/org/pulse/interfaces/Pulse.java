package org.pulse.interfaces;

import java.util.List;
import java.util.Optional;

public interface Pulse<S> extends ServerPulseRecordCleanerRegistry<S> {
	void loseHeartbeat();
	
	Optional<ServerPulseRecord<S>> getActiveServerPulseRecord();
	
	List<ServerPulseRecord<S>> getActiveServers();
}
