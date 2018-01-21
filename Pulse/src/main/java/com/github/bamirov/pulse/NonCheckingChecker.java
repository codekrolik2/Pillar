package com.github.bamirov.pulse;

import com.github.bamirov.pulse.interfaces.RecordDeleter;
import com.github.bamirov.pulse.interfaces.ServerChecker;
import com.github.bamirov.pulse.interfaces.ServerPulseRecord;

public class NonCheckingChecker<S> implements ServerChecker<S> {
	@Override
	public void checkServer(ServerPulseRecord<S> server, RecordDeleter<S> deleter) {
		deleter.deleteServer(server);
	}
}
