package com.github.bamirov.pulse.interfaces;

import java.util.List;
import java.util.Optional;

import com.github.bamirov.pillar.db.interfaces.TransactionContext;

/**
 * Abstract DAO that provides methods to manage server heartbeat records
 * 
 * @author bamirov
 *
 * @param <S> Server ID
 */
public interface ServerPulseDAO<S> {
	public ServerPulseRecord<S> createServer(TransactionContext tc, String registrationTime, long heartbeatPeriod, String serverInfo) throws Exception;
	public int deleteServer(TransactionContext tc, S serverId) throws Exception;

	public Optional<ServerPulseRecord<S>> getServer(TransactionContext tc, S serverId) throws Exception;
	public List<ServerPulseRecord<S>> getAllServers(TransactionContext tc) throws Exception;
	
	public int updateHeartbeat(TransactionContext tc, S serverId, String newHeartbeatTime, long heartbeatPeriod, String serverInfo) throws Exception;

	public Optional<ServerPulseRecord<S>> getRandomServer(TransactionContext tc) throws Exception;
}
