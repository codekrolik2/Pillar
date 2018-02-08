package org.pulse.interfaces;

import java.util.List;
import java.util.Optional;

public interface Pulse<S> extends ServerPulseListenerRegistry<S>, ServerPulseRecordCleanerRegistry<S> {
	void loseHeartbeat(Exception le);
	Optional<ServerPulseRecord<S>> getActiveServerPulseRecord();
	List<ServerPulseRecord<S>> getActiveServers();
}
