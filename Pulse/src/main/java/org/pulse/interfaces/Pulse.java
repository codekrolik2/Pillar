package org.pulse.interfaces;

import java.util.List;
import java.util.Optional;

import org.pillar.time.interfaces.Timestamp;

public interface Pulse<S> extends ServerPulseRecordCleanerRegistry<S> {
	boolean registerServerHB(String serverInfo, Timestamp currentTime);
	
	void loseHeartbeat();
	
	Optional<ServerPulseRecord<S>> getActiveServerPulseRecord();
	
	List<ServerPulseRecord<S>> getActiveServers();
}
