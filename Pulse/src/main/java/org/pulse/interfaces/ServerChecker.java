package org.pulse.interfaces;

/**
 * Checks whether a given server is healthy
 * 
 * @author bamirov
 *
 * @param <S> Server ID
 */
public interface ServerChecker<S> {
	/**
	 * (Asynchronously) checks whether a given server is healthy.
	 * If unhealthy, calls deleter.
	 */
	void checkServer(ServerPulseRecord<S> server, RecordDeleter<S> deleter);
}
