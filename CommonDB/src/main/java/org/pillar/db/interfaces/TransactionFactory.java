package org.pillar.db.interfaces;

public interface TransactionFactory {
	TransactionContext startTransaction() throws Exception;
	
	/**
	 * 
	 * @param transactionIsolationLevel e.g. Connection.TRANSACTION_READ_UNCOMMITTED
	 * @return
	 * @throws Exception
	 */
	TransactionContext startTransaction(int transactionIsolationLevel) throws Exception;
	
	//TransactionFactory switchDB(String newDBName);
}
