package org.pulse;

import java.util.concurrent.Executor;

import org.pulse.interfaces.RecordDeleter;
import org.pulse.interfaces.ServerChecker;
import org.pulse.interfaces.ServerPulseRecord;

public class AsyncChecker<S> implements ServerChecker<S> {
	protected Executor executor;
	protected ServerChecker<S> checker;
	
	public AsyncChecker(Executor executor, ServerChecker<S> checker) {
		this.executor = executor;
		this.checker = checker;
	}
	
	@Override
	public void checkServer(ServerPulseRecord<S> server, RecordDeleter<S> deleter) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				checker.checkServer(server, deleter);
			}
		});
	}
}