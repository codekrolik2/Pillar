package com.github.bamirov.tracker.interfaces;

import java.util.List;

public interface ObjectExpiryTracker<TI, A> {
	void objectConnected(TrackedObject<TI, A> object);
	void objectDisconnected(TI objectId);
	
	List<TrackedObject<TI, A>> getAllTrackedObjects();
	TrackedObject<TI, A> getTrackedObject(TI id);
	
	//------------------Entity Status Listeners------------------
	
    void addTrackedObjectListener(TrackedObjectListener<TI, A> broadcaster);
    boolean removeTrackedObjectListener(TrackedObjectListener<TI, A> broadcaster);
    void clearTrackedObjectListeners();
}