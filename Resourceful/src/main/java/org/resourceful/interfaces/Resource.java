package org.resourceful.interfaces;

import java.util.Optional;

public interface Resource<R, L> {
	R getResourceId();
	Optional<L> getLockerId();
	Optional<L> getLastLockCheck();
}
