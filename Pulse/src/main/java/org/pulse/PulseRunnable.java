package org.pulse;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.pillar.exec.CustomSleepPeriodRunnable;
import org.pillar.exec.interfaces.SleepTimeProvider;
import org.pillar.time.interfaces.TimeProvider;
import org.pillar.time.interfaces.Timestamp;
import org.pillar.wait.WaitStrategies;
import org.pillar.wait.interfaces.WaitStrategy;
import org.pulse.interfaces.PulseReg;

public class PulseRunnable<S> extends CustomSleepPeriodRunnable<Timestamp> {
    private PulseReg<S> pulse;
    private Supplier<String> serverInfoGetter;
    private TimeProvider timeProvider;
    
	public PulseRunnable(ScheduledExecutorService scheduler, PulseReg<S> pulse, Supplier<String> serverInfoGetter,
			TimeProvider timeProvider, long heartbeatFrequency, long minWait) {
		super(scheduler, new PulserTimeProvider(timeProvider, 
						WaitStrategies.exponentialWait(minWait, heartbeatFrequency, TimeUnit.MILLISECONDS),
						heartbeatFrequency));
		
		this.serverInfoGetter = serverInfoGetter;
		this.pulse = pulse;
		this.timeProvider = timeProvider;
	}
	
	@Override
	public Timestamp action() throws Exception {
		try {
			Timestamp hbTime = timeProvider.getCurrentTime();
			
			pulse.registerServerHB(serverInfoGetter.get(), hbTime);
			
			return hbTime;
		} catch (Exception e) {
			throw e;
		}
	}
}

class PulserTimeProvider implements SleepTimeProvider<Timestamp> {
	final Logger logger = Logger.getLogger(PulserTimeProvider.class);
	
    protected WaitStrategy waitStrategy;
    
    protected TimeProvider timeProvider;
    protected long heartbeatFrequency;
    protected int heartbeatRetryAttempts;
    protected long minWait;
    
    public PulserTimeProvider(TimeProvider timeProvider, WaitStrategy waitStrategy, 
    		long heartbeatFrequency) {
    	this.timeProvider = timeProvider;
    	this.waitStrategy = waitStrategy;
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
		
		if (sleepTime < 0)
			sleepTime = 0;
		return sleepTime;
	}
	
	@Override
	public long getExceptionSleepTime(Exception e) {
		//In case of error or otherwise lost heartbeat - Exponential wait
		long sleepTime = waitStrategy.computeSleepTime(heartbeatRetryAttempts++);
		logger.error(String.format("Exponential wait due to heartbeat loss; retry in %d ms", sleepTime), e);
		
		return sleepTime;
	}
}
