package com.github.bamirov.resourceful.exceptions;

public class LockException extends Exception {
	private static final long serialVersionUID = 5572266805569547825L;

	public LockException() {
		super();
	}

	public LockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LockException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockException(String message) {
		super(message);
	}

	public LockException(Throwable cause) {
		super(cause);
	}
}
