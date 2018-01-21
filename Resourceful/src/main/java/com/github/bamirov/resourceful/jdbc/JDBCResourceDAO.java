package com.github.bamirov.resourceful.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.bamirov.pillar.db.interfaces.TransactionContext;
import com.github.bamirov.pillar.db.jdbc.JDBCTransactionContext;
import com.github.bamirov.resourceful.interfaces.Resource;
import com.github.bamirov.resourceful.interfaces.ResourceDAO;

public class JDBCResourceDAO implements ResourceDAO<Long, Long> {
	protected String resourceTableName;

	protected String resourceIdColumnName;
	protected String lockerIdColumnName;
	protected String lastLockIdColumnName;

	protected String resourceParentsTableName;
	
	protected String resourceIdColumnNameParentsTable;
	protected String resourceParentIdColumnName;
	
	public JDBCResourceDAO(String resourceTableName, String resourceIdColumnName, String lockerIdColumnName, 
			String lastLockIdColumnName,
			String resourceParentsTableName, String resourceIdColumnNameParentsTable, String resourceParentIdColumnName) {
		this.resourceTableName = resourceTableName;
		this.resourceIdColumnName = resourceIdColumnName;
		this.lockerIdColumnName = lockerIdColumnName;
		this.lastLockIdColumnName = lastLockIdColumnName;
		
		this.resourceParentsTableName = resourceParentsTableName;
		this.resourceIdColumnNameParentsTable = resourceIdColumnNameParentsTable;
		this.resourceParentIdColumnName = resourceParentIdColumnName;
	}
	
	protected Resource<Long, Long> loadResource(ResultSet rs) throws SQLException {
		long resourceId = rs.getLong(0);//id_resource
		
		long rsLockerId = rs.getLong(1);//id_locker
		Optional<Long> lockerId = rs.wasNull() ? Optional.empty() : Optional.of(rsLockerId);
		
		long rsLastLockId = rs.getLong(2);//last_lock
		Optional<Long> lastLockCheck = rs.wasNull() ? Optional.empty() : Optional.of(rsLastLockId);
		
		return new ResourceImpl(resourceId, lockerId, lastLockCheck);
	}
	
	@Override
	public Optional<Resource<Long, Long>> getResource(TransactionContext tc, Long resourceId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("SELECT * FROM `%s` WHERE `%s` = ?", resourceTableName, resourceIdColumnName)
			);
			pst.setLong(1, resourceId);
			ResultSet rs = pst.executeQuery();
			
			if (rs.next())
				return Optional.of(loadResource(rs));
			else
				return Optional.empty();
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	@Override
	public int updateResourceLocker(TransactionContext tc, Long resourceId, Long lockerId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("UPDATE `%s` SET `%s` = ? WHERE `%s` = ?", resourceTableName, lockerIdColumnName, resourceIdColumnName)
			);
			pst.setLong(1, lockerId);
			pst.setLong(2, resourceId);
			return pst.executeUpdate();
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	@Override
	public int resetResourceLocker(TransactionContext tc, Long resourceId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("UPDATE `%s` SET `%s` = ? WHERE `%s` = ?", resourceTableName, lockerIdColumnName, resourceIdColumnName)
			);
			pst.setNull(1, java.sql.Types.BIGINT);
			pst.setLong(2, resourceId);
			
			return pst.executeUpdate();
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	@Override
	public int updateResourceLastLockCheck(TransactionContext tc, Long resourceId, Long lockerId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement(
				String.format("UPDATE `%s` SET `%s` = ? WHERE `%s` = ?", 
						resourceTableName, lastLockIdColumnName, resourceIdColumnName)
			);
			pst.setLong(1, lockerId);
			pst.setLong(2, resourceId);
			return pst.executeUpdate();
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	//--------------------------------------------------------------

	interface ResourceBFS {
	   List<Resource<Long, Long>> getNextLevel(TransactionContext tc, List<Long> previousLevel) throws SQLException;
	}
	
	@Override
	public Collection<Resource<Long, Long>> getResourcesChildrenRecursive(TransactionContext tc, Long resourceId) throws Exception {
		return getResourcesRecursive(tc, resourceId, (a,b) -> getChildren(a, b));
	}

	@Override
	public Collection<Resource<Long, Long>> getResourcesParentsRecursive(TransactionContext tc, Long resourceId) throws Exception {
		return getResourcesRecursive(tc, resourceId, (a,b) -> getParents(a, b));
	}

	protected Collection<Resource<Long, Long>> getResourcesRecursive(TransactionContext tc, Long resourceId, 
			ResourceBFS resourceBFS) throws Exception {
		List<Long> resources = new ArrayList<Long>();
		Map<Long, Resource<Long, Long>> children = new HashMap<>();
		
		resources.add(resourceId);
	
		while (!resources.isEmpty()) {
			List<Resource<Long, Long>> nextLevel = resourceBFS.getNextLevel(tc, resources);
			
			resources = new ArrayList<Long>();
			for (Resource<Long, Long> resource : nextLevel) {
				if (!children.containsKey(resource.getResourceId())) {
					children.put(resource.getResourceId(), resource);
					resources.add(resource.getResourceId());
				}
			}
		}
		
		return children.values();
	}
	
	public List<Resource<Long, Long>> getParents(TransactionContext tc, List<Long> previousLevel) throws SQLException {
		return getChildrenOrParents(tc, previousLevel, false);
	}
	
	public List<Resource<Long, Long>> getChildren(TransactionContext tc, List<Long> previousLevel) throws SQLException {
		return getChildrenOrParents(tc, previousLevel, true);
	}

	protected List<Resource<Long, Long>> getChildrenOrParents(TransactionContext tc, List<Long> previousLevel,
			boolean isChildren) throws SQLException {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			StringBuilder queryBuilder = new StringBuilder();
			
			queryBuilder.append(
				String.format("SELECT r.* FROM `%s` r, `%s` p WHERE r.`%s` = p.`%s` AND p.`%s` IN (", 
						resourceTableName, resourceParentsTableName, resourceIdColumnName, 
						isChildren ? resourceIdColumnNameParentsTable : resourceParentIdColumnName, 
						isChildren ? resourceParentIdColumnName : resourceIdColumnNameParentsTable)
			);
			
			for (int i = 0; i < previousLevel.size(); i++)
				queryBuilder.append(i == 0 ? "?" : ", ?");
			
			queryBuilder.append(")");
			
			pst = connection.prepareStatement(queryBuilder.toString());
			
			for (int i = 0; i < previousLevel.size(); i++)
				pst.setLong(i, previousLevel.get(i));

			ResultSet rs = pst.executeQuery();
			
			List<Resource<Long, Long>> retList = new ArrayList<>();
			while (rs.next())
				retList.add(loadResource(rs));
			
			return retList;
		} finally {
			if (pst != null) pst.close();
		}
	}
}
