package org.pulse;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.pillar.exec.CustomSleepPeriodRunnable;
import org.pillar.exec.interfaces.SleepTimeProvider;
import org.pillar.time.interfaces.TimeProvider;
import org.pillar.time.interfaces.Timestamp;
import org.pillar.wait.interfaces.WaitStrategy;
import org.pulse.interfaces.Pulse;
import org.pulse.interfaces.ServerPulseListener;
import org.pulse.interfaces.ServerPulseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulseRunnable<S> extends CustomSleepPeriodRunnable<Timestamp> {
    private static final Logger logger = LoggerFactory.getLogger(PulseRunnable.class);
	
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
		try {
			Timestamp hbTime = timeProvider.getCurrentTime();
			
			boolean heartbeatSuccess = pulse.registerServerHB(serverInfoGetter.get(), hbTime);
			if (!heartbeatSuccess)
				throw new Exception("Heartbeat lost");
			
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
				logger.error("ServerPulseListener exception", e);
			}
    }

    protected void notifyFailedHB(Exception e) {
    	for (ServerPulseListener<S> pulseListener : serverPulseListeners)
			try {
				pulseListener.hbFailed(e);
			} catch (Exception e1) {
				logger.error("ServerPulseListener exception", e1);
			}
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
