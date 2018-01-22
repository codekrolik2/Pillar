package org.resourceful.exceptions;

import lombok.Getter;

public class ResourceLockerMismatchException extends LockException {
	private static final long serialVersionUID = 4380071882049334912L;

	@Getter
	protected Object lockerId;
	
	public ResourceLockerMismatchException(Object lockerId) {
		super();
	}

	public ResourceLockerMismatchException(Object lockerId, String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceLockerMismatchException(Object lockerId, String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceLockerMismatchException(Object lockerId, String message) {
		super(message);
	}

	public ResourceLockerMismatchException(Object lockerId, Throwable cause) {
		super(cause);
	}
}
