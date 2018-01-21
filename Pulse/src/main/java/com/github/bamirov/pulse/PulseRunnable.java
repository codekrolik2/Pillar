package com.github.bamirov.pulse;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bamirov.pillar.exec.CustomSleepPeriodRunnable;
import com.github.bamirov.pillar.exec.interfaces.SleepTimeProvider;
import com.github.bamirov.pillar.time.interfaces.TimeProvider;
import com.github.bamirov.pillar.time.interfaces.Timestamp;
import com.github.bamirov.pillar.wait.interfaces.WaitStrategy;
import com.github.bamirov.pulse.interfaces.Pulse;

public class PulseRunnable<S> extends CustomSleepPeriodRunnable<Timestamp> {
	private Pulse<S> pulse;
    private Supplier<String> serverInfoGetter;
    private TimeProvider timeProvider;
    
	public PulseRunnable(ScheduledExecutorService scheduler, Pulse<S> pulse, Supplier<String> serverInfoGetter,
			TimeProvider timeProvider, long heartbeatFrequency) {
		super(scheduler, new PulserTimeProvider(timeProvider, heartbeatFrequency));
		
		this.serverInfoGetter = serverInfoGetter;
		this.pulse = pulse;
		this.timeProvider = timeProvider;
	}
	
	@Override
	public Timestamp action() throws Exception {
		Timestamp hbTime = timeProvider.getCurrentTime();
		
		boolean heartbeatSuccess = pulse.registerServerHB(serverInfoGetter.get(), hbTime);
		if (!heartbeatSuccess)
			throw new Exception("Heartbeat lost");

		return hbTime;
	}
}

class PulserTimeProvider implements SleepTimeProvider<Timestamp> {
	final Logger logger = LoggerFactory.getLogger(PulserTimeProvider.class);
	
    protected WaitStrategy waitStrategy;
    
    protected TimeProvider timeProvider;
    protected long heartbeatFrequency;
    protected int heartbeatRetryAttempts;
    
    public PulserTimeProvider(TimeProvider timeProvider, long heartbeatFrequency) {
    	this.timeProvider = timeProvider;
    	this.heartbeatFrequency = heartbeatFrequency;
    }
    
	@Override
	public long getSuccessSleepTime(Timestamp hbTime) {
		heartbeatRetryAttempts = 0;
		
		Timestamp timeAllDone = timeProvider.getCurrentTime();
		
		long delta = hbTime.getDeltaInMs(timeAllDone);//finishTime - startTime
		
		long sleepTime;
		if (delta < 0)
			sleepTime = heartbeatFrequency;
		else if (delta > heartbeatFrequency)
			sleepTime = 0;
		else
			sleepTime = heartbeatFrequency - delta;
		
		return sleepTime;
	}
	
	@Override
	public long getExceptionSleepTime(Exception e) {
		//In case of error or otherwise lost heartbeat - Exponential wait
		long sleepTime = waitStrategy.computeSleepTime(heartbeatRetryAttempts++);
		logger.error("Exponential wait due to heartbeat loss; retry in " + sleepTime + " ms", e);
		
		return sleepTime;
	}
}
