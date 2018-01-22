package org.pillar.time;

import org.pillar.time.interfaces.TimeProvider;
import org.pillar.time.interfaces.Timestamp;

public class LongTimeProvider implements TimeProvider {
	@Override
	public Timestamp createTimestamp(String rawTime) {
		return new LongTimestamp(Long.parseLong(rawTime));
	}

	@Override
	public Timestamp getCurrentTime() {
		return new LongTimestamp(System.currentTimeMillis());
	}

	@Override
	public Timestamp getTheBeginningOfTime() {
		return new LongTimestamp(0);
	}
}
