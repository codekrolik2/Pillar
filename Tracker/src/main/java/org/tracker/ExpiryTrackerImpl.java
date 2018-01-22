package org.tracker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tracker.interfaces.ObjectExpiryTracker;
import org.tracker.interfaces.TrackedObject;
import org.tracker.interfaces.TrackedObjectListener;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ExpiryTrackerImpl<TI, A> implements ObjectExpiryTracker<TI, A> {
    private static final Logger logger = LoggerFactory.getLogger(ExpiryTrackerImpl.class);
    
	@AllArgsConstructor
	protected class TrackedObjectInfo {
		@Getter
		TrackedObject<TI, A> object;
	}
	
	protected long expirationTimeoutMs;
	protected ReentrantLock lock;
	protected Map<TI, TrackedObjectInfo> objects;

	protected final ScheduledExecutorService scheduler;
	
	public ExpiryTrackerImpl(ScheduledExecutorService scheduler, long expirationTimeoutMs) {
		this.scheduler = scheduler;
		
		this.expirationTimeoutMs = expirationTimeoutMs;
		lock = new ReentrantLock();
		objects = new ConcurrentHashMap<TI, TrackedObjectInfo>();
	}
	
	public ExpiryTrackerImpl(long expirationTimeoutMs) {
		this(Executors.newSingleThreadScheduledExecutor(), expirationTimeoutMs);
	}
	
	//--------------------------
	
	public void objectConnected(TrackedObject<TI, A> object) {
		lock.lock();
		try {
			TrackedObjectInfo oldObjectInfo = objects.get(object.getId());
			objects.put(object.getId(), new TrackedObjectInfo(object));
			
			if (oldObjectInfo == null)
				notifyObjectConnected(object);
			else
				notifyObjectReconnected(oldObjectInfo.getObject(), object);
		} finally {
			lock.unlock();
		}
	}
	
	protected class ObjectExpirationJob implements Runnable {
		TI objectId;
		TrackedObjectInfo targetObject;
		
		public ObjectExpirationJob(TI objectId, TrackedObjectInfo targetObject) {
			this.objectId = objectId;
			this.targetObject = targetObject;
		}
		
		@Override
		public void run() {
			lock.lock();
			try {
				TrackedObjectInfo mapObjectInfo = objects.get(objectId);
				if (mapObjectInfo.equals(targetObject)) {
					objects.remove(objectId);
					notifyObjectExpired(targetObject.getObject());
				}
			} finally {
				lock.unlock();
			}
		}
	}
	
	public void objectDisconnected(TI objectId) {
		lock.lock();
		try {
			TrackedObjectInfo targetObject = objects.get(objectId);
			scheduler.schedule(new ObjectExpirationJob(objectId, targetObject), expirationTimeoutMs, TimeUnit.MILLISECONDS);
			notifyObjectDisconnected(targetObject.getObject());
		} finally {
			lock.unlock();
		}
	}
	
	public List<TrackedObject<TI, A>> getAllTrackedObjects() {
		lock.lock();
		try {
			return objects.values().stream().map(t -> t.getObject()).collect(Collectors.toList());
		} finally {
			lock.unlock();
		}
	}
	
	public TrackedObject<TI, A> getTrackedObject(TI id) {
		lock.lock();
		try {
			TrackedObjectInfo obj = objects.get(id);
			return obj == null ? null : obj.getObject();
		} finally {
			lock.unlock();
		}
	}
	
	//--------------------------
	
	private Set<TrackedObjectListener<TI, A>> trackedObjectListeners = 
			Collections.newSetFromMap(new ConcurrentHashMap<TrackedObjectListener<TI, A>, Boolean>());
	
    public void addTrackedObjectListener(TrackedObjectListener<TI, A> broadcaster) {
    	trackedObjectListeners.add(broadcaster);
    }

    public boolean removeTrackedObjectListener(TrackedObjectListener<TI, A> broadcaster) {
		return trackedObjectListeners.remove(broadcaster);
    }

    public void clearTrackedObjectListeners() {
    	trackedObjectListeners.clear();
    }

    private void notifyObjectConnected(TrackedObject<TI, A> o) {
    	for (TrackedObjectListener<TI, A> trackedObjectListener : trackedObjectListeners)
			try {
	    		trackedObjectListener.connected(o);
			} catch (Exception e) {
				logger.error("TrackedObjectListener exception", e);
			}
    }

    private void notifyObjectDisconnected(TrackedObject<TI, A> o) {
    	for (TrackedObjectListener<TI, A> trackedObjectListener : trackedObjectListeners)
			try {
				trackedObjectListener.disconnected(o);
			} catch (Exception e) {
				logger.error("TrackedObjectListener exception", e);
			}
    }

    private void notifyObjectReconnected(TrackedObject<TI, A> oldObj, TrackedObject<TI, A> newObj) {
    	for (TrackedObjectListener<TI, A> trackedObjectListener : trackedObjectListeners)
			try {
	    		trackedObjectListener.reconnected(oldObj, newObj);
			} catch (Exception e) {
				logger.error("TrackedObjectListener exception", e);
			}
    }

    private void notifyObjectExpired(TrackedObject<TI, A> o) {
    	for (TrackedObjectListener<TI, A> trackedObjectListener : trackedObjectListeners)
			try {
    			trackedObjectListener.expired(o);
			} catch (Exception e) {
				logger.error("TrackedObjectListener exception", e);
			}
    }
}
