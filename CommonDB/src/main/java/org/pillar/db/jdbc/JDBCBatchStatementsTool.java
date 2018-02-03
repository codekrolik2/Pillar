package org.pillar.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class JDBCBatchStatementsTool {
	public static void deleteSchema(Connection connection, String dbName) throws SQLException {
		String escapedDbName = dbName.replace("'","''");
		String creationScript = "DROP SCHEMA " + escapedDbName + ";";
		
		PreparedStatement pst = null;
        try {
        	pst = connection.prepareStatement(creationScript);
            pst.executeUpdate();
        } catch(SQLException se) {
            //errors for JDBC
            se.printStackTrace();
        } finally {
            if (pst != null) { pst.close(); }
        }
	}

	public static void executeStatementsBatch(Connection connection, Iterator<String> statementsIter) throws SQLException {
		Statement s = connection.createStatement();
		
		while (statementsIter.hasNext()) {
			String statement = statementsIter.next();
			
			s.addBatch(statement);
		}
		
	    s.executeBatch();
	}
	
	public static Iterator<String> getStatementsIterator(final String batch) {
		Iterator<String> statementsIterator = new Iterator<String>() {
			String nextStatement = null;
			int cursor = 0;
			
			@Override
			public String next() {
				String oldStatement = nextStatement;
				findNext();
				return oldStatement;
			}
			
			@Override
			public boolean hasNext() {
				if (nextStatement == null)
					return findNext();
				else
					return true;
			}
			
			private boolean findNext() {
				if (cursor >= batch.length()) {
					nextStatement = null;
					return false;
				}
				
				StringBuilder statementBuilder = new StringBuilder();
				
				while ((cursor < batch.length()) && (batch.charAt(cursor) != ';')) {
					statementBuilder.append(batch.charAt(cursor));
					cursor++;
				}
				cursor++;
				
				String statement = statementBuilder.toString().trim();
				
				//System.out.println("--\n\n"+statement+"\n\n--");
				
				if (!statement.trim().equals("")) {
					nextStatement = statement;
					return true;
				} else
					return findNext();
			}
			
			@Override
			public void remove() {
			}
		};
		
		return statementsIterator;
	}
}
