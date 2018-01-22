package org.pulse.interfaces;

public interface ServerPulseListener<S> {
	void hbSuccessful(ServerPulseRecord<S> server); 
	void hbFailed(Exception e); 
}
