package org.pulse;

import org.pillar.db.interfaces.TransactionContext;
import org.pulse.interfaces.ServerPulseDAO;
import org.pulse.interfaces.ServerPulseRecordCleaner;

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
