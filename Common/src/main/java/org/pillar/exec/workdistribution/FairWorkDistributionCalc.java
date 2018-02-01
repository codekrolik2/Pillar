package org.pillar.exec.workdistribution;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Given the following information:
 * 1. workers
 * 2. number of units of work currently owned by each worker
 * 3. workers' work capacity limits (if any)
 * 4. number of units of work available in the pool to be distributed
 *  
 * the algorithm calculates how much units of work should be loaded by a particular worker,
 * so that work distribution among all workers stays fair.
 * Communication between workers is not required, the information indicated above is sufficient
 * for any of the participating workers to make an informed decision.
 * 
 * For details check "Job distribution algo.pdf" in src/main/resources
 * 
 */
public class FairWorkDistributionCalc {
	class OwnedJobsComparator implements Comparator<Worker> {
		@Override
		public int compare(Worker o1, Worker o2) {
			return Long.compare(o1.getOwnedWorkUnits(), o2.getOwnedWorkUnits());
		}
	}
	
	class JobLimitComparator implements Comparator<Worker> {
		@Override
		public int compare(Worker o1, Worker o2) {
			return Long.compare(o1.getWorkUnitLimit().get(), o2.getWorkUnitLimit().get());
		}
	}
	
	public long calculate(long availableUnitsOfWork, Collection<Worker> workers, Worker self) {
		if (self == null)
			throw new IllegalArgumentException("Parameter \"self\" can't be null");
		
		boolean exists = false;
		for (Worker worker : workers) {
			if (worker == self) {
				exists = true;
				break;
			}
		}
		
		if (!exists)
			throw new IllegalArgumentException("Object \"self\" doesn't exist in \"workers\"");
		
		PriorityQueue<Worker> workerQ = new PriorityQueue<Worker>(new OwnedJobsComparator());
		for (Worker worker : workers)
			workerQ.add(worker);
		
		PriorityQueue<Worker> limitQ = new PriorityQueue<Worker>(new JobLimitComparator());
		
		long uow = availableUnitsOfWork;
		long total = 0L;
		Set<Worker> workingSet = new HashSet<>();
		
		while (true) {
			if (uow == 0L)
				break;
			
			if (workingSet.isEmpty()) {
				Worker firstWorker = workerQ.poll();
				workingSet.add(firstWorker);
				
				total = firstWorker.getOwnedWorkUnits();
				
				if (firstWorker.getWorkUnitLimit().isPresent())
					limitQ.add(firstWorker);
			} else {
				boolean useLimitQ = !limitQ.isEmpty() &&
					(
						workerQ.isEmpty() 
						||
						limitQ.peek().getWorkUnitLimit().get() <= workerQ.peek().getOwnedWorkUnits() 
					);
				
				boolean useWorkerQ = !workerQ.isEmpty() &&
					(
						limitQ.isEmpty() 
						||
						workerQ.peek().getOwnedWorkUnits() < limitQ.peek().getWorkUnitLimit().get() 
					);
				
				if (useLimitQ) {
					Worker limitWorker = limitQ.peek();
					long forEach = limitWorker.getWorkUnitLimit().get() - total;
					
					long toDistribute = forEach * workingSet.size();
					if (toDistribute <= uow) {
						total += forEach;
						uow -= toDistribute;
						
						limitQ.poll();
						
						if (limitWorker == self)
							break;
						
						workingSet.remove(limitWorker);
						continue;
					}
				} else if (useWorkerQ) {
					Worker nextWorker = workerQ.peek();
					
					long forEach = nextWorker.getOwnedWorkUnits() - total;
					long toDistribute = forEach * workingSet.size();
					
					if (toDistribute <= uow) {
						total += forEach;
						uow -= toDistribute;
						
						workerQ.poll();
						workingSet.add(nextWorker);
						
						if (nextWorker.getWorkUnitLimit().isPresent())
							limitQ.add(nextWorker);
						
						continue;
					}
				} 
				
				{
					//both empty or not enough units of work left to reach next 
					long forEach = uow / workingSet.size();
					total += forEach;
					uow -= forEach * workingSet.size();
					break;
				}
			}
		}
		
		if ((!self.getWorkUnitLimit().isPresent()) || (self.getWorkUnitLimit().get() > total))
			if (uow > 0L)
				total++;
		
		long toLoad = total - self.getOwnedWorkUnits();
		return (toLoad > 0L) ? toLoad : 0L;
	}
}
