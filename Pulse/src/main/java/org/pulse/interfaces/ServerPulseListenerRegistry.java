package org.pulse.interfaces;

import java.util.Collection;

public interface ServerPulseListenerRegistry<S> {
    void addServerPulseListener(ServerPulseListener<S> pulseListener);
    boolean removeServerPulseListener(ServerPulseListener<S> pulseListener);
    void clearServerPulseListener();
	Collection<ServerPulseListener<S>> getServerPulseListeners();
}
