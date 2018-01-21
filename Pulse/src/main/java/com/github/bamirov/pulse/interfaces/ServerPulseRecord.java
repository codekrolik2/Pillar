package com.github.bamirov.pulse.interfaces;

import com.github.bamirov.pillar.time.interfaces.Timestamp;

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
