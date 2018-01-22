package org.pillar.db.jdbc;

import java.sql.Connection;

import org.pillar.db.interfaces.TransactionContext;

import lombok.Getter;

public class JDBCTransactionContext implements TransactionContext {
	@Getter
	protected Connection connection;
	
	public JDBCTransactionContext(Connection connection) {	
		this.connection = connection;
	}
	
	@Override
	public void commit() throws Exception {
		connection.commit();
	}

	@Override
	public void rollback() throws Exception {
		connection.rollback();
	}

	@Override
	public void close() throws Exception {
		connection.close();
	}
}
