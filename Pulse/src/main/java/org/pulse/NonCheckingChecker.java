package org.pulse;

import org.pulse.interfaces.RecordDeleter;
import org.pulse.interfaces.ServerChecker;
import org.pulse.interfaces.ServerPulseRecord;

public class NonCheckingChecker<S> implements ServerChecker<S> {
	@Override
	public void checkServer(ServerPulseRecord<S> server, RecordDeleter<S> deleter) {
		deleter.deleteServer(server);
	}
}
