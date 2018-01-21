package com.github.bamirov.pulse.interfaces;

@FunctionalInterface
public interface RecordDeleter<S> {
	void deleteServer(ServerPulseRecord<S> server);
}
