package org.pulse.interfaces;

import org.pillar.db.interfaces.TransactionContext;

/**
 * Contains logic to delete server heartbeat record
 * 
 * @author bamirov
 *
 * @param <S> Server ID
 */
public interface ServerPulseRecordCleaner<S> {
	public void deleteServer(TransactionContext tc, S serverId) throws Exception;
}
