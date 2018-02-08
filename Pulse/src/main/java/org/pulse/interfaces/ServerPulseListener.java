package org.pulse.interfaces;

public interface ServerPulseListener<S> {
	void hbCreated(ServerPulseRecord<S> server);
	void hbUpdated(ServerPulseRecord<S> server); 
	void hbLost(Exception e);
}
