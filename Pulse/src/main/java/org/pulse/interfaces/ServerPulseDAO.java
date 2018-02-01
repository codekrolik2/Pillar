package org.pulse.interfaces;

import java.util.List;
import java.util.Optional;

import org.pillar.db.interfaces.TransactionContext;
import org.pillar.time.interfaces.Timestamp;

/**
 * Abstract DAO that provides methods to manage server heartbeat records
 * 
 * @author bamirov
 *
 * @param <S> Server ID
 */
public interface ServerPulseDAO<S> {
	public ServerPulseRecord<S> createServer(TransactionContext tc, Timestamp registrationTime, long heartbeatPeriod, String serverInfo) throws Exception;
	public int deleteServer(TransactionContext tc, S serverId) throws Exception;

	public Optional<ServerPulseRecord<S>> getServer(TransactionContext tc, S serverId) throws Exception;
	public List<ServerPulseRecord<S>> getAllServers(TransactionContext tc) throws Exception;
	
	public int updateHeartbeat(TransactionContext tc, S serverId, Timestamp newHeartbeatTime, long heartbeatPeriod, String serverInfo) throws Exception;

	public Optional<ServerPulseRecord<S>> getRandomServer(TransactionContext tc) throws Exception;
}
