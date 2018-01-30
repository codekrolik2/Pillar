package org.pillar.exec.work;

import org.apache.log4j.Logger;

public abstract class WorkThreadPoolRunnable<W> implements Runnable {
	final Logger logger = Logger.getLogger(WorkThreadPoolRunnable.class);
	
	public WorkThreadPool<W> pool;
	
	public WorkThreadPoolRunnable(WorkThreadPool<W> pool) {
		this.pool = pool;
	}
	
	public void run() {
		while (true) {
			W work = null;
			try {
				work = pool.getUnitOfWork();
			} catch (InterruptedException e) { }
			
			if (work != null) {
				try {
					process(work);
				} catch (Exception e) {
					logger.error("Processing exception occured", e);
				}
			}
			
			if (pool.requestShutdown())
				break;
		}
	}
	
	public abstract void process(W work);
}
