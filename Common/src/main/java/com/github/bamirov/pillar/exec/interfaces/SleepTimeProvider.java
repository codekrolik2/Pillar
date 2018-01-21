package com.github.bamirov.pillar.exec.interfaces;

public interface SleepTimeProvider<R> {
	/**
	 * Get sleep time based on return value of action
	 * 
	 * @param actionReturnValue return value of action
	 * @return sleep time or -1 to stop execution
	 */
	long getSuccessSleepTime(R actionReturnValue);
	
	/**
	 * Get sleep time based on exception occured
	 * 
	 * @param e exception
	 * @return sleep time or -1 to stop execution
	 */
	long getExceptionSleepTime(Exception e);
}
