package org.pulse.interfaces;

import org.pillar.time.interfaces.Timestamp;

public interface PulseReg<S> extends Pulse<S> {
	void registerServerHB(String serverInfo, Timestamp currentTime) throws Exception;
}
