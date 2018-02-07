package org.pulse;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.pulse.interfaces.ServerPulseListener;
import org.pulse.interfaces.ServerPulseRecord;

public class PulseRunnable<S> extends CustomSleepPeriodRunnable<Timestamp> {
    private static final Logger logger = Logger.getLogger(PulseRunnable.class);
	
    private PulseReg<S> pulse;
    private Supplier<String> serverInfoGetter;
    private TimeProvider timeProvider;
    
	public PulseRunnable(ScheduledExecutorService scheduler, PulseReg<S> pulse, Supplier<String> serverInfoGetter,
			TimeProvider timeProvider, long heartbeatFrequency) {
		super(scheduler, 
				new PulserTimeProvider(timeProvider, 
						WaitStrategies.exponentialWait(heartbeatFrequency, TimeUnit.MILLISECONDS),
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
			
			notifySuccessfulHB(pulse.getActiveServerPulseRecord().get());
			
			return hbTime;
		} catch (Exception e) {
			notifyFailedHB(e);
			throw e;
		}
	}
	
	//--------------------------
	
	private Set<ServerPulseListener<S>> serverPulseListeners = 
			Collections.newSetFromMap(new ConcurrentHashMap<ServerPulseListener<S>, Boolean>());
	
    public void addServerPulseListener(ServerPulseListener<S> pulseListener) {
    	serverPulseListeners.add(pulseListener);
    }

    public boolean removeServerPulseListener(ServerPulseListener<S> pulseListener) {
		return serverPulseListeners.remove(pulseListener);
    }

    public void clearServerPulseListener() {
    	serverPulseListeners.clear();
    }

    protected void notifySuccessfulHB(ServerPulseRecord<S> server) {
    	for (ServerPulseListener<S> pulseListener : serverPulseListeners)
			try {
				pulseListener.hbSuccessful(server);
			} catch (Exception e) {
				logger.error("ServerPulseListener.hbSuccessful exception", e);
			}
    }

    protected void notifyFailedHB(Exception e) {
    	for (ServerPulseListener<S> pulseListener : serverPulseListeners)
			try {
				pulseListener.hbFailed(e);
			} catch (Exception e1) {
				logger.error("ServerPulseListener.hbFailed exception", e1);
			}
    }
}

class PulserTimeProvider implements SleepTimeProvider<Timestamp> {
	final Logger logger = Logger.getLogger(PulserTimeProvider.class);
	
    protected WaitStrategy waitStrategy;
    
    protected TimeProvider timeProvider;
    protected long heartbeatFrequency;
    protected int heartbeatRetryAttempts;
    
    public PulserTimeProvider(TimeProvider timeProvider, WaitStrategy waitStrategy, long heartbeatFrequency) {
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
