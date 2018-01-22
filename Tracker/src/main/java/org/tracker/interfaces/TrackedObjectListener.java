package org.tracker.interfaces;

public interface TrackedObjectListener<TI, A> {
	void connected(TrackedObject<TI, A> o) throws Exception;
	void disconnected(TrackedObject<TI, A> o) throws Exception;
	void reconnected(TrackedObject<TI, A> oldObj, TrackedObject<TI, A> newObj) throws Exception;
	
	void expired(TrackedObject<TI, A> o) throws Exception;
}
