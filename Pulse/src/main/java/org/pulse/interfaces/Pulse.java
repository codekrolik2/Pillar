package org.pulse.interfaces;

import java.util.List;
import java.util.Optional;

import org.pillar.time.interfaces.Timestamp;

public interface Pulse<S> extends ServerPulseRecordCleanerRegistry<S> {
	void registerServerHB(String serverInfo, Timestamp currentTime) throws Exception;
	
	void loseHeartbeat();
	
	Optional<ServerPulseRecord<S>> getActiveServerPulseRecord();
	
	List<ServerPulseRecord<S>> getActiveServers();
}
