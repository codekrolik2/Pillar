package org.pillar.time.interfaces;

/**
 * Provides abstract timestamps
 * 
 * @author bamirov
 *
 */
public interface TimeProvider {
	Timestamp createTimestamp(String rawTime);
	Timestamp getCurrentTime();
	Timestamp getTheBeginningOfTime();
}
