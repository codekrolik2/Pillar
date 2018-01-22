package org.pillar.time.interfaces;

import java.util.concurrent.TimeUnit;

/**
 * Timestamp
 * 
 * @author bamirov
 *
 */
public interface Timestamp extends Comparable<Timestamp> {
	String getRawTime();
	Timestamp shiftBy(long time, TimeUnit unit);
	
	/**
	 * @return o - this
	 */
	long getDeltaInMs(Timestamp o);
}
