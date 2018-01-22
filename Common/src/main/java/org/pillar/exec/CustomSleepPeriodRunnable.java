package org.pillar.exec;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.pillar.exec.interfaces.SleepTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CustomSleepPeriodRunnable<R> implements Runnable {
	final Logger logger = LoggerFactory.getLogger(CustomSleepPeriodRunnable.class);
	
	protected ScheduledExecutorService scheduler;
	protected SleepTimeProvider<R> stp;
	
	public CustomSleepPeriodRunnable(ScheduledExecutorService scheduler, SleepTimeProvider<R> stp) {
		this.scheduler = scheduler;
		this.stp = stp;
	}
	
	@Override
	public void run() {
		long sleepTime = -1;
		try {
			R result = action();
			sleepTime = stp.getSuccessSleepTime(result);
		} catch (Exception e) {
			sleepTime = stp.getExceptionSleepTime(e);
		} finally {
			if (sleepTime > 0)
				scheduler.schedule(this, sleepTime, TimeUnit.MILLISECONDS);
		}
	}
	
	public abstract R action() throws Exception;
}
