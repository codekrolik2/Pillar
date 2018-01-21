package com.github.bamirov.pulse;

import java.util.concurrent.Executor;

import com.github.bamirov.pulse.interfaces.RecordDeleter;
import com.github.bamirov.pulse.interfaces.ServerChecker;
import com.github.bamirov.pulse.interfaces.ServerPulseRecord;

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