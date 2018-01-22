package org.pulse.interfaces;

import org.pillar.time.interfaces.Timestamp;

/**
 * Server record that Heartbeat creates
 * 
 * @author bamirov
 *
 * @param <S> Server ID
 */
public interface ServerPulseRecord<S> {
	S getServerId();
	String getInfo();
	
	Timestamp getCreationTime();
	Timestamp getLastHBTime();
	long getHBPeriodMs();
}
