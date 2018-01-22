package org.resourceful.jdbc;

import java.util.Optional;

import org.resourceful.interfaces.Resource;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResourceImpl implements Resource<Long, Long> {
	protected Long resourceId;
	protected Optional<Long> lockerId;
	protected Optional<Long> lastLockCheck;
}
