package org.pulse.interfaces;

@FunctionalInterface
public interface RecordDeleter<S> {
	void deleteServer(ServerPulseRecord<S> server);
}
