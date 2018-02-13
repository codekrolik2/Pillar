package org.pillar.db.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.pillar.db.interfaces.TransactionContext;
import org.pillar.db.interfaces.TransactionFactory;

public class JDBCTransactionFactory implements TransactionFactory {
	public static final int transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;

	protected String url;
	protected String user;
	protected String password;
	
	public JDBCTransactionFactory(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}
	
	public TransactionContext startTransaction() throws Exception {
		return new JDBCTransactionContext(connect());
	}
	
	public TransactionContext startTransaction(int transactionIsolationLevel) throws Exception {
		return new JDBCTransactionContext(connect(transactionIsolationLevel));
	}
	
	protected Connection connect() throws SQLException {
		return connect(transactionIsolationLevel);
	}
	
	protected Connection connect(int transactionIsolationLevel) throws SQLException {
		Connection connection = DriverManager.getConnection(url, user, password);
		connection.setAutoCommit(false);
		connection.setTransactionIsolation(transactionIsolationLevel);
		return connection;
	}
	
	protected TransactionFactory switchDB(String newDBName) {
		int questionPos = url.indexOf('?');
		String query = url.substring(questionPos);
		
		String head = url.substring(0, questionPos);
		head = head.substring(0, head.lastIndexOf('/')+1);
		
		String newURI = head + newDBName + query;
		
		return new JDBCTransactionFactory(newURI, user, password);
	}
	
	//----------------------------------------
	
	public static long getLastId(Connection connection) throws SQLException {
		PreparedStatement pst = null;
        
        try {
            pst = connection.prepareStatement("SELECT LAST_INSERT_ID();");
            ResultSet rs = pst.executeQuery();
            
            long id = -1;
            while (rs.next())
            	id = rs.getLong(1);
            
            return id;
        } finally {
            if (pst != null) { pst.close(); }
        }
	}
	
	public static long getFoundRows(Connection connection) throws SQLException {
		PreparedStatement pst = null;
        
        try {
            pst = connection.prepareStatement("SELECT FOUND_ROWS();");
            ResultSet rs = pst.executeQuery();
            
            long id = -1;
            while (rs.next())
            	id = rs.getLong(1);
            
            return id;
        } finally {
            if (pst != null) { pst.close(); }
        }
	}
	
	public static Connection connectMySQL(String hosts, String user, String password, String dbName) throws SQLException {
		String url = createMySQLUrl(hosts, dbName);
		
		Connection connection = DriverManager.getConnection(url, user, password);
		connection.setTransactionIsolation(transactionIsolationLevel);
		connection.setAutoCommit(false);
		return connection;
	}
	
	public static String createMySQLUrl(String hosts, int connectTimeout, int socketTimeout) {
		return "jdbc:mysql:loadbalance://" + hosts + "?connectTimeout=" + connectTimeout + "&socketTimeout=" + socketTimeout;
	}
	
	public static String createMySQLUrl(String hosts, String dbName, int connectTimeout, int socketTimeout) {
		return "jdbc:mysql:loadbalance://" + hosts + "/" + dbName + "?connectTimeout=" + connectTimeout + "&socketTimeout=" + socketTimeout;
	}
	
	public static String createMySQLUrl(String hosts) {
		return "jdbc:mysql:loadbalance://" + hosts + "?connectTimeout=" + 10000 + "&socketTimeout=" + 10000;
	}
	
	public static String createMySQLUrl(String hosts, String dbName) {
		return "jdbc:mysql:loadbalance://" + hosts + "/" + dbName + "?connectTimeout=" + 10000 + "&socketTimeout=" + 10000;
	}
}
