package org.pillar.exec.work;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.pillar.time.interfaces.TimeProvider;
import org.pillar.time.interfaces.Timestamp;

public class WorkThreadPool<W> {
	protected AtomicInteger desiredThreadCount;
	protected AtomicInteger threadCount;
	protected Set<Thread> threads;
	protected TimeProvider timeProvider;
	
	protected PriorityQueue<DelayedWork> q;
	protected ReentrantLock waitLock;
	protected Condition waitCondition;
	
	protected AtomicInteger jobsInFlight = new AtomicInteger(0);
	protected AtomicInteger jobsInQ = new AtomicInteger(0);
	
	protected WorkThreadPoolRunnableFactory<W> runnableFactory;
	
	public WorkThreadPool(TimeProvider timeProvider, WorkThreadPoolRunnableFactory<W> runnableFactory) {
		desiredThreadCount = new AtomicInteger(-1);
		this.threadCount = new AtomicInteger(0);
		threads = new HashSet<>();
		waitLock = new ReentrantLock();
		waitCondition = waitLock.newCondition();
		this.timeProvider = timeProvider;
		this.runnableFactory = runnableFactory;
		
		q = new PriorityQueue<>();
	}
	
	class DelayedWork implements Comparable<DelayedWork> {
		W work;
		long delayMS;
		Timestamp acceptTime;
		ReentrantLock lock;
		
		public DelayedWork(W work, long delayMS, Timestamp acceptTime) {
			this.work = work;
			this.delayMS = delayMS;
			this.acceptTime = acceptTime;
			lock = new ReentrantLock();
		}
		
		public long getDelay() {
			boolean timeJumpDetected = false;
			
			lock.lock();
			try {
				Timestamp currentTime = timeProvider.getCurrentTime();

				//If time jumps back, best we can do is to pretend that we've just accepted the work and wait our delay again.
				//Much worse would be to wait for additional (acceptTime - currentTime) on top of our delay.
				if (currentTime.compareTo(acceptTime) < 0) {
					timeJumpDetected = true;
					acceptTime = currentTime;
				}
				
				//Also to minimize the effect of possible time jump back we reduce delay every time this method is called 
				//to keep track of elapsed time.
				//In this manner the elapsed time that was tracked won't be a part of additional wait caused 
				//by time jumping back
				delayMS -= (acceptTime.getDeltaInMs(currentTime));
				acceptTime = currentTime;
				
			} finally {
				lock.unlock();
			}
			
			//Also make sure all other jobs' times are shifted if necessary
			if (timeJumpDetected)
				for (DelayedWork w : q)
					w.getDelay();
		
			return delayMS;
		}

		@Override
		public int compareTo(DelayedWork o) {
			return Long.compare(delayMS, o.delayMS);
		}
	}
	
	public void addUnitOfWork(W work, long delayMS) {
		waitLock.lock();
        try {
        	q.add(new DelayedWork(work, delayMS, timeProvider.getCurrentTime()));
        	jobsInQ.incrementAndGet();
        	waitCondition.signal();
        } finally {
            waitLock.unlock();
        }
	}
	
	Thread leader = null;
	public W getUnitOfWork() throws InterruptedException {
		waitLock.lock();
        try {
            while (true) {
            	DelayedWork first = q.peek();
                if (first == null)
                    waitCondition.await();
                else {
                    long delay = first.getDelay();
                    if (delay <= 0) {
                    	jobsInQ.decrementAndGet();
                        return q.poll().work;
                    }

                    if (leader != null)
                    	waitCondition.await();
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                        	waitCondition.awaitNanos(delay);
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
            	waitCondition.signal();
            waitLock.unlock();
        }
	}

	protected boolean spawnThread() {
		waitLock.lock();
		try {
			if (threadCount.get() < desiredThreadCount.get()) {
				WorkThreadPoolRunnable<W> runnable = runnableFactory.createRunnable();
				Thread thread = new Thread(runnable);
				thread.start();
				threads.add(thread);
				threadCount.incrementAndGet();
				return true;
			} else
				return false;
		} finally {
			waitLock.unlock();
		}
	}
	
	public boolean requestShutdown() {
		if (threadCount.get() <= desiredThreadCount.get())
			return false;
		
		waitLock.lock();
		try {
			if (!threads.contains(Thread.currentThread()))
				throw new IllegalArgumentException("Thread pool doesn't contain Current Thread");
			
			if (threadCount.get() > desiredThreadCount.get()) {
				threads.remove(Thread.currentThread());
				threadCount.decrementAndGet();
				return true;
			} else
				return false;
		} finally {
			waitLock.unlock();
		}
	}
	
	public void start(int threadCount) {
		adjustThreadCount(threadCount);
	}
	
	public void adjustThreadCount(int newDesiredThreadCount) {
		if (newDesiredThreadCount < 1)
			throw new IllegalArgumentException("To remove all threads call shutdown()");
		
		waitLock.lock();
		try {
			if (desiredThreadCount.get() == 0)
				throw new IllegalStateException("Thread Pool was shut down");
			
			desiredThreadCount.set(newDesiredThreadCount);
			
			if (threadCount.get() < desiredThreadCount.getAndIncrement()) {
				for (int i = threadCount.get(); i < desiredThreadCount.get(); i++) {
					spawnThread();
				}
			} else {
				waitCondition.signalAll();
			}
		} finally {
			waitLock.unlock();
		}
	}
	
	public void shutdown() {
		waitLock.lock();
		try {
			desiredThreadCount.set(0);
			waitCondition.signalAll();
		} finally {
			waitLock.unlock();
		}
	}
	
	public void interruptAllThreads() {
		waitLock.lock();
		try {
			for (Thread t : threads)
				t.interrupt();
		} finally {
			waitLock.unlock();
		}
	}
	
	public int getThreadCount() {
		return threadCount.get();
	}
	
	public int getDesiredThreadCount() {
		return desiredThreadCount.get();
	}
	
	public boolean isShutdown() {
		return desiredThreadCount.get() <= 0;
	}
	
	public int getJobsInQ() {
		return jobsInQ.get();
	}
	
	public int getJobsInFlight() {
		return jobsInFlight.get();
	}
	
	public void incrementInFlight() {
		jobsInFlight.incrementAndGet();
	}
	
	public void decrementInFlight() {
		jobsInFlight.decrementAndGet();
	}
}
