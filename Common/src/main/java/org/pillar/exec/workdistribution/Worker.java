package org.pillar.exec.workdistribution;

import java.util.Optional;

public interface Worker {
	long getOwnedWorkUnits();
	Optional<Long> getWorkUnitLimit();
}
