package com.github.bamirov.pulse.interfaces;

public interface ServerPulseListener<S> {
	void hbSuccessful(ServerPulseRecord<S> server); 
	void hbFailed(Exception e); 
}
