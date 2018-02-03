package org.pulse.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.pillar.db.interfaces.TransactionContext;
import org.pillar.db.jdbc.JDBCTransactionContext;
import org.pillar.db.jdbc.JDBCTransactionFactory;
import org.pillar.time.LongTimeProvider;
import org.pillar.time.LongTimestamp;
import org.pillar.time.interfaces.Timestamp;
import org.pulse.interfaces.ServerPulseDAO;
import org.pulse.interfaces.ServerPulseRecord;

import lombok.Getter;
import lombok.Setter;

public class JDBCServerPulseDAO implements ServerPulseDAO<Long> {
	@Getter @Setter
	protected String serversTable = "servers";
	@Getter @Setter
	protected String idServerCol = "id_server";
	@Getter @Setter
	protected String registrationTimeCol = "registration_time";
	@Getter @Setter
	protected String lastHeartbeatTimeCol = "last_heartbeat_time";
	@Getter @Setter
	protected String heartbeatPeriodCol = "heartbeat_period";
	@Getter @Setter
	protected String serverInfoCol = "server_info";
	
	protected LongTimeProvider timeProvider;
	
	public JDBCServerPulseDAO(LongTimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}
	
	protected ServerPulseRecord<Long> loadServerPulseRecord(ResultSet rs) throws SQLException {
		Long serverId = rs.getLong(1);
		Timestamp creationTime = timeProvider.createTimestamp(rs.getLong(2));
		Timestamp lastHBTime = timeProvider.createTimestamp(rs.getLong(3));
		long hbPeriodMs = rs.getLong(4);
		String info = rs.getString(5);
		if (rs.wasNull())
			info = null;
		
		return new JDBCServerPulseRecord(serverId, info, creationTime, lastHBTime, hbPeriodMs);
	}
	
	@Override
	public ServerPulseRecord<Long> createServer(TransactionContext tc, Timestamp registrationTime, long heartbeatPeriod,
			String serverInfo) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("INSERT INTO `%s` (`%s`, `%s`, `%s`, `%s`) VALUES (?, ?, ?, ?)", 
						serversTable, registrationTimeCol, lastHeartbeatTimeCol, heartbeatPeriodCol, serverInfoCol)
			);
			pst.setLong(1, ((LongTimestamp)registrationTime).getTimeMs());
			pst.setLong(2, ((LongTimestamp)registrationTime).getTimeMs());
			pst.setLong(3, heartbeatPeriod);
			pst.setString(4, serverInfo);
			
			pst.executeUpdate();
			
			long serverId = JDBCTransactionFactory.getLastId(connection);
			return getServer(tc, serverId).get();
		} finally {
			if (pst != null) pst.close();
		}
	}

	@Override
	public int deleteServer(TransactionContext tc, Long serverId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("DELETE FROM `%s` WHERE `%s` = ?", serversTable, idServerCol)
			);
			pst.setLong(1, serverId);
			
			return pst.executeUpdate();
		} finally {
			if (pst != null) pst.close();
		}
	}

	@Override
	public Optional<ServerPulseRecord<Long>> getServer(TransactionContext tc, Long serverId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("SELECT `%s`, `%s`, `%s`, `%s`, `%s` FROM `%s` WHERE `%s` = ?", 
						idServerCol, registrationTimeCol, lastHeartbeatTimeCol, 
						heartbeatPeriodCol, serverInfoCol, serversTable, idServerCol)
			);
			pst.setLong(1, serverId);
			ResultSet rs = pst.executeQuery();
			
			if (rs.next())
				return Optional.of(loadServerPulseRecord(rs));
			else
				return Optional.empty();
		} finally {
			if (pst != null) pst.close();
		}
	}

	@Override
	public List<ServerPulseRecord<Long>> getAllServers(TransactionContext tc) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("SELECT `%s`, `%s`, `%s`, `%s`, `%s` FROM `%s`", 
						idServerCol, registrationTimeCol, lastHeartbeatTimeCol, 
						heartbeatPeriodCol, serverInfoCol, serversTable)
			);
			ResultSet rs = pst.executeQuery();
			
			List<ServerPulseRecord<Long>> servers = new ArrayList<ServerPulseRecord<Long>>();
			while (rs.next())
				servers.add(loadServerPulseRecord(rs));
			
			return servers;
		} finally {
			if (pst != null) pst.close();
		}
	}

	@Override
	public int updateHeartbeat(TransactionContext tc, Long serverId, Timestamp newHeartbeatTime, long heartbeatPeriod,
			String serverInfo) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("UPDATE `%s` SET `%s` = ?, `%s` = ?, `%s` = ? WHERE `%s` = ?", 
						serversTable, 
						lastHeartbeatTimeCol,
						heartbeatPeriodCol,
						serverInfoCol,
						idServerCol)
			);
			
			pst.setLong(1, ((LongTimestamp)newHeartbeatTime).getTimeMs());
			pst.setLong(2, heartbeatPeriod);
			pst.setString(3, serverInfo);
			pst.setLong(4, serverId);
			
			return pst.executeUpdate();
		} finally {
			if (pst != null) pst.close();
		}
	}

	//SELECT col1 FROM tbl ORDER BY RAND() LIMIT 1;
	
	//TODO: test the following
	//SELECT col1 FROM tbl LIMIT RAND()*(SELECT count(col1) FROM tbl), 1; ???
	@Override
	public Optional<ServerPulseRecord<Long>> getRandomServer(TransactionContext tc) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("SELECT `%s`, `%s`, `%s`, `%s`, `%s` FROM `%s` ORDER BY RAND() LIMIT 1;", 
						idServerCol, registrationTimeCol, lastHeartbeatTimeCol, 
						heartbeatPeriodCol, serverInfoCol, serversTable)
			);
			ResultSet rs = pst.executeQuery();
			
			if (rs.next())
				return Optional.of(loadServerPulseRecord(rs));
			else
				return Optional.empty();
		} finally {
			if (pst != null) pst.close();
		}
	}
}
