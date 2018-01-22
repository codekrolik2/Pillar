package org.resourceful.dao;

import java.util.Collection;
import java.util.Optional;

import org.pillar.db.interfaces.TransactionContext;
import org.resourceful.exceptions.LockException;
import org.resourceful.exceptions.ResourceLockerMismatchException;
import org.resourceful.exceptions.ResourceNotLockedException;
import org.resourceful.interfaces.Resource;
import org.resourceful.interfaces.ResourceDAO;

public class LockDAO<R, L> {
	protected ResourceDAO<R, L> rDao;
	
	public LockDAO(ResourceDAO<R, L> rDao) {
		this.rDao = rDao;
	}

	public void lockResource(TransactionContext tc, R resourceId, L lockerId) throws LockException, Exception {
		Optional<Resource<R, L>> res = rDao.getResource(tc, resourceId);
		
		if ((res == null) || (!res.isPresent())) {
			throw new LockException(
					String.format("Can't lock resource [%s] with locker [%s], Resource not found.",
							resourceId.toString(), lockerId.toString())
				);
		}
		
		if (res.get().getLockerId().isPresent()) {
			throw new LockException(
					String.format("Can't lock resource [%s] with locker [%s], Resource already locked by locker [%s].",
							resourceId.toString(), lockerId.toString(), res.get().getLockerId().get().toString())
				);
		}
		
		Collection<Resource<R, L>> children = rDao.getResourcesChildrenRecursive(tc, resourceId);
		for (Resource<R, L> child : children) {
			if (child.getLockerId().isPresent())
				throw new LockException(
						String.format("Can't lock resource [%s] with locker [%s], Resource's Child [%s] already locked by locker [%s].",
								resourceId.toString(), lockerId.toString(), child.getResourceId(), child.getLockerId().get().toString())
					);
		}
		
		Collection<Resource<R, L>> parents = rDao.getResourcesParentsRecursive(tc, resourceId);
		for (Resource<R, L> parent : parents) {
			if (parent.getLockerId().isPresent())
				throw new LockException(
						String.format("Can't lock resource [%s] with locker [%s], Resource's Parent [%s] already locked by locker [%s].",
								resourceId.toString(), lockerId.toString(), parent.getResourceId(), parent.getLockerId().get().toString())
					);
		}
		
		rDao.updateResourceLocker(tc, resourceId, lockerId);
		
		for (Resource<R, L> child : children)
			rDao.updateResourceLastLockCheck(tc, child.getResourceId(), lockerId);
		
		for (Resource<R, L> parent : parents)
			rDao.updateResourceLastLockCheck(tc, parent.getResourceId(), lockerId);
	}

	public void unlockResource(TransactionContext tc, R resourceId, L lockerId) throws LockException, Exception {
		Optional<Resource<R, L>> res = rDao.getResource(tc, resourceId);
		
		if ((res == null) || (res.isPresent())) {
			throw new LockException(
					String.format("Can't unlock resource [%s] with locker [%s], Resource not found.",
							resourceId.toString(), lockerId.toString())
				);
		}
		
		if (!res.get().getLockerId().isPresent()) {
			throw new ResourceNotLockedException(
					String.format("Can't unlock resource [%s] with locker [%s], Resource not locked.",
							resourceId.toString(), lockerId.toString(), res.get().getLockerId().get().toString())
				);
		}
		
		if (!res.get().getLockerId().equals(lockerId)) {
			throw new ResourceLockerMismatchException(lockerId,
					String.format("Can't unlock resource [%s] with locker [%s], Resource locked by a different locker: [%s].",
							resourceId.toString(), lockerId.toString(), res.get().getLockerId().get().toString())
				);
		}
		
		rDao.resetResourceLocker(tc, resourceId);
	}
}
