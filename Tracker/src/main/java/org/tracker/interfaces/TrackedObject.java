package org.tracker.interfaces;

public interface TrackedObject<TI, A> {
	TI getId();
	
	boolean isActive();
	void close();
	
	A getAttachment();
}
