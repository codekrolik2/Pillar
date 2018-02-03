package org.pillar.exec.work;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.pillar.time.interfaces.TimeProvider;
import org.pillar.time.interfaces.Timestamp;

public abstract class WorkThreadPool<W> {
	protected AtomicInteger desiredSize;
	protected AtomicInteger size;
	protected Set<Thread> threads;
	protected TimeProvider timeProvider;
	
	protected PriorityQueue<DelayedWork> q;
	protected ReentrantLock waitLock;
	protected Condition waitCondition;
	
	public WorkThreadPool(TimeProvider timeProvider) {
		desiredSize = new AtomicInteger(-1);
		this.size = new AtomicInteger(0);
		threads = new HashSet<>();
		waitLock = new ReentrantLock();
		waitCondition = waitLock.newCondition();
		this.timeProvider = timeProvider;
		
		q = new PriorityQueue<>();
	}
	
	public WorkThreadPool(int size, TimeProvider timeProvider) {
		this(timeProvider);
		adjustSize(size);
	}
	
	protected abstract WorkThreadPoolRunnable<W> createRunnable();
	
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
                    if (delay <= 0)
                        return q.poll().work;

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
			if (size.get() < desiredSize.get()) {
				WorkThreadPoolRunnable<W> runnable = createRunnable();
				Thread thread = new Thread(runnable);
				thread.start();
				threads.add(thread);
				size.incrementAndGet();
				return true;
			} else
				return false;
		} finally {
			waitLock.unlock();
		}
	}
	
	public boolean requestShutdown() {
		if (size.get() <= desiredSize.get())
			return false;
		
		waitLock.lock();
		try {
			if (!threads.contains(Thread.currentThread()))
				throw new IllegalArgumentException("Thread pool doesn't contain Current Thread");
			
			if (size.get() > desiredSize.get()) {
				threads.remove(Thread.currentThread());
				size.decrementAndGet();
				return true;
			} else
				return false;
		} finally {
			waitLock.unlock();
		}
	}
	
	public void adjustSize(int newDesiredSize) {
		if (newDesiredSize < 1)
			throw new IllegalArgumentException("To remove all threads call shutdown()");
		
		waitLock.lock();
		try {
			if (desiredSize.get() == 0)
				throw new IllegalStateException("Thread Pool was shut down");
			
			desiredSize.set(newDesiredSize);
			
			if (size.get() < desiredSize.getAndIncrement()) {
				for (int i = size.get(); i < desiredSize.get(); i++) {
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
			desiredSize.set(0);
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
	
	public int getSize() {
		return size.get();
	}
	
	public int getDesiredSize() {
		return desiredSize.get();
	}
	
	public boolean isShutdown() {
		return desiredSize.get() <= 0;
	}
}
