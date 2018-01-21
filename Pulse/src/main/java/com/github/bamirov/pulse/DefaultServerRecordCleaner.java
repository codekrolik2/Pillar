package com.github.bamirov.pulse;

import com.github.bamirov.pillar.db.interfaces.TransactionContext;
import com.github.bamirov.pulse.interfaces.ServerPulseDAO;
import com.github.bamirov.pulse.interfaces.ServerPulseRecordCleaner;

public class DefaultServerRecordCleaner<S> implements ServerPulseRecordCleaner<S> {
	ServerPulseDAO<S> dao;
	
	public DefaultServerRecordCleaner(ServerPulseDAO<S> dao) {
		this.dao = dao;
	}
	
	@Override
	public void deleteServer(TransactionContext tc, S serverId) throws Exception {
		dao.deleteServer(tc, serverId);
	}
}
