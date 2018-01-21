package com.github.bamirov.pillar.time;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.github.bamirov.pillar.time.interfaces.Timestamp;

public class LongTimestamp implements Timestamp {
	private long timeMs;
	
	public LongTimestamp(long timeMs) {
		this.timeMs = timeMs;
	}
	
	@Override
	public int compareTo(Timestamp o) {
		return Long.compare(timeMs, ((LongTimestamp)o).timeMs);
	}

	@Override
	public String getRawTime() {
		return Long.toString(timeMs);
	}

	@Override
	public Timestamp shiftBy(long time, TimeUnit unit) {
		return new LongTimestamp(timeMs + unit.toMillis(time));
	}

	@Override
	public long getDeltaInMs(Timestamp o) {
		return ((LongTimestamp)o).timeMs - timeMs;
	}

	@Override
	public String toString() {
		Date d = new Date(timeMs);
		return d.toString() + " [" + timeMs + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timeMs ^ (timeMs >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LongTimestamp other = (LongTimestamp) obj;
		if (timeMs != other.timeMs)
			return false;
		return true;
	}
}
