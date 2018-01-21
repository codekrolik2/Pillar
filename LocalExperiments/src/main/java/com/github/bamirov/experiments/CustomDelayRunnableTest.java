package com.github.bamirov.experiments;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CustomDelayRunnableTest {
	public static void main(String[] args) {
		class DelayableRunnable implements Runnable {
			private long delay;
			private ScheduledExecutorService service;
			
			private long maxDelay = 10000;
			
			public DelayableRunnable(long delay, ScheduledExecutorService service) {
				if (delay > maxDelay)
					delay = maxDelay;
				this.delay = delay;
					
				this.service = service;
			}
			
			@Override
			public void run() {
				System.out.println(String.format("Running and rescheduling with delay %d", delay));
				service.schedule(new DelayableRunnable(delay*2, service), delay, TimeUnit.MILLISECONDS);
			}
		};
		
		ScheduledExecutorService service = Executors.newScheduledThreadPool(4);
		
		service.execute(new DelayableRunnable(10, service));
	}
}
