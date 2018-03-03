package org.pillar.time;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.pillar.time.interfaces.Timestamp;

import lombok.Getter;

public class LongTimestamp implements Timestamp {
	@Getter
	private long unixTime;
	
	public LongTimestamp(long unixTime) {
		this.unixTime = unixTime;
	}
	
	@Override
	public int compareTo(Timestamp o) {
		return Long.compare(unixTime, ((LongTimestamp)o).unixTime);
	}

	@Override
	public String getRawTime() {
		return Long.toString(unixTime);
	}

	@Override
	public Timestamp shiftBy(long time, TimeUnit unit) {
		return new LongTimestamp(unixTime + unit.toMillis(time));
	}

	@Override
	public long getDeltaInMs(Timestamp o) {
		return ((LongTimestamp)o).unixTime - unixTime;
	}

	@Override
	public String toString() {
		Date d = new Date(unixTime);
		return d.toString() + " [" + unixTime + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (unixTime ^ (unixTime >>> 32));
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
		if (unixTime != other.unixTime)
			return false;
		return true;
	}
}
