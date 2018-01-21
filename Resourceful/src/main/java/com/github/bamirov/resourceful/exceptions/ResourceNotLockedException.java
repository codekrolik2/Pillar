package com.github.bamirov.resourceful.exceptions;

public class ResourceNotLockedException extends LockException {
	private static final long serialVersionUID = 7030606440477211498L;

	public ResourceNotLockedException() {
		super();
	}

	public ResourceNotLockedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceNotLockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceNotLockedException(String message) {
		super(message);
	}

	public ResourceNotLockedException(Throwable cause) {
		super(cause);
	}
}
