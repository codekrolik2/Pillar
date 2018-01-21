package com.github.bamirov.resourceful.interfaces;

import java.util.Collection;
import java.util.Optional;

import com.github.bamirov.pillar.db.interfaces.TransactionContext;

public interface ResourceDAO<R, L> {
	Optional<Resource<R, L>> getResource(TransactionContext tc, R resourceId) throws Exception;
	
	Collection<Resource<R, L>> getResourcesChildrenRecursive(TransactionContext tc, R resourceId) throws Exception;
	Collection<Resource<R, L>> getResourcesParentsRecursive(TransactionContext tc, R resourceId) throws Exception;
	
	int updateResourceLocker(TransactionContext tc, R resourceId, L lockerId) throws Exception;
	int updateResourceLastLockCheck(TransactionContext tc, R resourceId, L lockerId) throws Exception;

	int resetResourceLocker(TransactionContext tc, R resourceId) throws Exception;
}
