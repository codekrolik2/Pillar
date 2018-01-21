package com.github.bamirov.pulse;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bamirov.pillar.db.interfaces.TransactionContext;
import com.github.bamirov.pillar.db.interfaces.TransactionFactory;
import com.github.bamirov.pillar.time.interfaces.Timestamp;
import com.github.bamirov.pulse.interfaces.Pulse;
import com.github.bamirov.pulse.interfaces.ServerChecker;
import com.github.bamirov.pulse.interfaces.ServerPulseDAO;
import com.github.bamirov.pulse.interfaces.ServerPulseRecord;
import com.github.bamirov.pulse.interfaces.ServerPulseRecordCleaner;

public class DefaultPulse<S> extends ServerPulseRecordCleanerRegistryImpl<S> implements Pulse<S> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPulse.class);
    
    private class ServerHbHistory {
    	Timestamp lastHeartbeatTime;
    	
    	int localChecks;
    	long maxLocalChecksWithoutUpdate;
    	
    	public final ServerPulseRecord<S> server;
    	
    	ServerHbHistory(ServerPulseRecord<S> server, Timestamp lastHeartbeatTime, long hbPeriod) {
    		this.server = server;
			this.lastHeartbeatTime = lastHeartbeatTime;
			
			this.localChecks = 0;
			maxLocalChecksWithoutUpdate = hbPeriod*maxMissedHeartbeats / heartbeatPeriod;
			if (maxLocalChecksWithoutUpdate < 2)
				maxLocalChecksWithoutUpdate = 2;
		}
    	
    	void updateHB(Timestamp newHeartbeatTime, long newHBPeriod, int localHBPeriod) {
    		maxLocalChecksWithoutUpdate = newHBPeriod*maxMissedHeartbeats / heartbeatPeriod;
			if (maxLocalChecksWithoutUpdate < 2)
				maxLocalChecksWithoutUpdate = 2;
    		
    		if (newHeartbeatTime.equals(lastHeartbeatTime))
    			localChecks++;
    		else
    			localChecks = 0;
    	}
    }
    
	private static final long maxMissedHeartbeats = 3L;
	
    private ServerPulseRecord<S> localServer;
    private AtomicReference<ConcurrentHashMap<S, ServerHbHistory>> activeServers;
    protected ServerPulseDAO<S> serverDAO;
    protected TransactionFactory connectionFactory;
    protected ServerChecker<S> serverChecker;
    protected int heartbeatPeriod;
    
    public DefaultPulse(TransactionFactory connectionFactory, ServerPulseDAO<S> serverDAO, ServerChecker<S> serverChecker,
    		int heartbeatPeriod) {
	    this.connectionFactory = connectionFactory;
	    this.serverDAO = serverDAO;
	    this.serverChecker = serverChecker;
	    this.heartbeatPeriod = heartbeatPeriod;
	    
	    if (serverChecker == null)
	    	logger.warn("WARNING Pulse init: ServerChecker is NULL!");
	    
    	localServer = null;
	    activeServers = new AtomicReference<ConcurrentHashMap<S, ServerHbHistory>>();
	    
	    this.addServerRecordCleaner(new DefaultServerRecordCleaner<S>(serverDAO));
	    
	    clearServerDefinitions();
    }
    
    protected ServerPulseRecord<S> createServer(TransactionContext tc, Timestamp registrationTime, long heartbeatPeriod, String serverInfo) throws Exception {
		return serverDAO.createServer(tc, registrationTime.toString(), heartbeatPeriod, serverInfo);
    }
    
    protected void updateServerHeartbeat(TransactionContext tc, S serverId, Timestamp newHeartbeatTime, long heartbeatPeriod, String serverInfo) throws Exception {
		serverDAO.updateHeartbeat(tc, serverId, newHeartbeatTime.toString(), heartbeatPeriod, serverInfo);
    }
    
    public List<ServerPulseRecord<S>> getActiveServers() {
		ConcurrentHashMap<S, ServerHbHistory> map = activeServers.get();
		List<ServerPulseRecord<S>> servers = map.values().stream().map(srv -> srv.server).collect(Collectors.toList());
		
		return servers;
    }

	@Override
	public Optional<ServerPulseRecord<S>> getActiveServerPulseRecord() {
		return Optional.ofNullable(localServer);
	}
    
    private void updateServerDefinitions(List<ServerPulseRecord<S>> newServers, long localHeartbeatPeriod) {
    	ConcurrentHashMap<S, ServerHbHistory> serverMap = activeServers.get();
    	
    	//1. Identify and remove missing servers
    	
    	//Identify servers missing from the current set
    	HashMap<S, ServerPulseRecord<S>> newServersById = new HashMap<S, ServerPulseRecord<S>>();
    	for (ServerPulseRecord<S> newServer : newServers)
    		newServersById.put(newServer.getServerId(), newServer);
    	
    	List<S> missingServers = new ArrayList<S>();
    	for (S oldServerId : serverMap.keySet()) {
    		if (!newServersById.containsKey(oldServerId))
    			missingServers.add(oldServerId);
    	}
    	
    	//Remove missing servers
    	for (S missingServerId : missingServers)
    		serverMap.remove(missingServerId);
    	
    	//2. Update servers heartbeat info - increments localChecks for abandoned records 
    	for (ServerPulseRecord<S> newServer : newServers) {
    		ServerHbHistory def = serverMap.get(newServer.getServerId());
    		if (def != null) {
    			def.updateHB(newServer.getLastHBTime(), newServer.getHBPeriodMs(), heartbeatPeriod);
    		} else {
    			ServerHbHistory newDef = new ServerHbHistory(newServer, newServer.getLastHBTime(), newServer.getHBPeriodMs());
    			serverMap.put(newServer.getServerId(), newDef);
    		}
    	}
    }
    
    Timestamp previousUpdateTime = null;
    void testHBSanity(Timestamp currentTime) throws Exception {
    	if (previousUpdateTime != null) {
	    	Timestamp projectedUpdateTime = previousUpdateTime.shiftBy(heartbeatPeriod + (long)(maxMissedHeartbeats*heartbeatPeriod), TimeUnit.MILLISECONDS);
	    	if (
		    		(localServer != null) && 
		    		(
		    			(currentTime.compareTo(projectedUpdateTime) > 0) ||
		    			(currentTime.compareTo(previousUpdateTime) < 0)
		    		)
		    ) {
	    		//Heartbeat lost due to thread delay - wasn't able to run heartbeat for (1.5 * updateFrequency).
				throw new Exception("Heartbeat lost due to Heartbeat abnormality. " +
						"[projectedUpdateTime " + projectedUpdateTime.getRawTime() + "; currentTime " + currentTime.getRawTime() + 
						"; previousUpdateTime " + previousUpdateTime.getRawTime() + "; heartbeatPeriod " + heartbeatPeriod + "]");
	    	}
    	}
    }
    
	public boolean registerServerHB(String serverInfo, Timestamp currentTime) {
		TransactionContext t = null;
        try {
        	testHBSanity(currentTime);
        	
    		String heartbeatMessage = "HB start: " + new Date();
    		
			//1) TXN1: Update pulse record and refresh pulse records list
    		t = connectionFactory.startTransaction();
        	if (localServer == null) {
        		//register pulse record
        		localServer = createServer(t, currentTime, heartbeatPeriod, serverInfo);
        		
        		heartbeatMessage = "Server registered: " + localServer.getServerId() + " - currentTime [" + currentTime.getRawTime() + "] " + heartbeatMessage;
        		
        		t.commit();
        	} else {
        		//update pulse record
        		updateServerHeartbeat(t, localServer.getServerId(), currentTime, heartbeatPeriod, serverInfo);
        		
        		heartbeatMessage = "Server HB updated: " + localServer.getServerId() + " - currentTime [" + currentTime.getRawTime() + "] " + heartbeatMessage;
        		
        		t.commit();
        	}
        	//done updating
        	t.close();
        	
        	
        	//2) TXN2: Get All pulse records and update local server definitions
        	t = connectionFactory.startTransaction();
        	
        	List<ServerPulseRecord<S>> servers = serverDAO.getAllServers(t);
        	
        	//Update server heartbeat info - increments localChecks for abandoned records
        	updateServerDefinitions(servers, heartbeatPeriod);

        	boolean localServerFound = false;
        	
        	//4) Check if our server record exists in the list
        	for (ServerPulseRecord<S> server : servers)
        		if (server.getServerId().equals(localServer.getServerId()))
        			localServerFound = true;
        	
        	if (!localServerFound)
        		throw new Exception("Sanity check failed: server record not found serverId " + localServer.getServerId());
			
        	logger.debug(heartbeatMessage);
	        
        	return true;
	    } catch(SQLException e) {
    		logger.error("Heartbeat lost due to SQL exception:", e);

    		try {
	    		if (t != null)
	    			t.rollback();
	    	} catch(Exception e1) {
	    		logger.error("Error while trying to roll back a transaction", e1);
	    	}
    		
    		loseHeartbeat();
    		
	    	return false;
	    } catch(Exception e) {
    		logger.error("Heartbeat lost due to exception:", e);

    		//reset serverId and clear active servers
    		loseHeartbeat();
    		
	    	return false;
	    } finally {
	    	try {
		    	if (t != null)
		    		t.close();
	    	} catch(Exception e1) {
	    		logger.error("Error while trying to close a SQL connection", e1);
	    	}
	    	
	    	//Check server records, try to determine lost records
	        try {
	        	ConcurrentHashMap<S, ServerHbHistory> serverMap = activeServers.get();
	        	
	        	for (ServerHbHistory serverDef : serverMap.values()) {
	        		logger.info(serverDef.server.getServerId() + " : " + serverDef.localChecks + " : " + serverDef.maxLocalChecksWithoutUpdate);
	        		
	        		if (serverDef.localChecks >= serverDef.maxLocalChecksWithoutUpdate) {
        		    	//Attempt to delete a record for a server that missed more than [heartbeatsBeforeMarkdown] heartbeats
        				ServerPulseRecord<S> server = serverDef.server;
        		    	
        				serverChecker.checkServer(server, this::deleteServer);
	        		}
	        	}
	    	} catch(Exception e1) {
	    		//Even if deletion of expired coordinators failed, it's still a success
	    		logger.error("Error while trying to delete expired coordinators", e1);
	    	}
    	}
	}
	
    private void clearServerDefinitions() {
    	activeServers.set(new ConcurrentHashMap<S, ServerHbHistory>());
    }
	
	public void loseHeartbeat() {
		localServer = null;
		previousUpdateTime = null;
		clearServerDefinitions();
	}
	
	//---------------------------------------------
	
    protected void deleteServer(ServerPulseRecord<S> server) {
		TransactionContext tc = null;
		try {
			tc = connectionFactory.startTransaction();

			for (ServerPulseRecordCleaner<S> cleaner : getServerRecordCleaners())
    			cleaner.deleteServer(tc, server.getServerId());
    		
        	tc.commit();
	    } catch(Exception e) {
    		logger.error("Expired server record deletion failure: [" + "id: " + server.getServerId() + " : " + server.getInfo(), e);

    		try {
	    		if (tc != null)
	    			tc.rollback();
	    	} catch(Exception e1) {
	    		logger.error("Error while trying to rollback a connection", e1);
	    	}
	    } finally {
	    	try {
		    	if (tc != null)
		    		tc.close();
	    	} catch(Exception e1) {
	    		logger.error("Error while trying to close a SQL connection", e1);
	    	}
	    }
    	
    	logger.info("Expired server record deleted | id: " + server.getServerId() + " - " + server.getInfo());
    }
}
