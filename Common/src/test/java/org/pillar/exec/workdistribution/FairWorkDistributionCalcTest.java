package org.pillar.exec.workdistribution;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class FairWorkDistributionCalcTest {
	@AllArgsConstructor
	class MyWorker implements Worker {
		@Getter
		long ownedWorkUnits;
		@Getter
		Optional<Long> workUnitLimit;
		
		public MyWorker(long ownedJobs) { this(ownedJobs, Optional.empty()); }
	}
	
	@Test
	public void test() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 17;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(5));
		workers.add(new MyWorker(25));
		workers.add(new MyWorker(10));
		workers.add(new MyWorker(12));

		assertEquals(10L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(5L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(3L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}
	
	@Test
	public void test2() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 6;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(15));
		workers.add(new MyWorker(25));
		workers.add(new MyWorker(10));
		workers.add(new MyWorker(12));

		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(4L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(2L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}
	
	@Test
	public void test3() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 3;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(14));
		workers.add(new MyWorker(25));
		workers.add(new MyWorker(10));
		workers.add(new MyWorker(15));

		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(3L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}

	@Test
	public void test4() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 17;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(10, Optional.of(13L)));
		workers.add(new MyWorker(25, Optional.of(50L)));
		workers.add(new MyWorker(12, Optional.of(16L)));
		workers.add(new MyWorker(5, Optional.of(50L)));

		assertEquals(3L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(4L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(11L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}

	@Test
	public void test5() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 17;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(10, Optional.of(13L)));
		workers.add(new MyWorker(25, Optional.of(50L)));
		workers.add(new MyWorker(12, Optional.of(13L)));
		workers.add(new MyWorker(5, Optional.of(50L)));

		assertEquals(3L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(1L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(13L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}

	@Test
	public void test6() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 1700;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(10, Optional.of(13L)));
		workers.add(new MyWorker(25, Optional.of(50L)));
		workers.add(new MyWorker(12, Optional.of(13L)));
		workers.add(new MyWorker(5, Optional.of(50L)));

		assertEquals(3L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(25L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(1L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(45L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}

	@Test
	public void test7() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 64;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(10, Optional.of(13L)));
		workers.add(new MyWorker(25, Optional.of(50L)));
		workers.add(new MyWorker(12, Optional.of(13L)));
		workers.add(new MyWorker(5, Optional.of(50L)));

		assertEquals(3L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(20L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(1L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(40L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}

	@Test
	public void test8() {
		FairWorkDistributionCalc calc = new FairWorkDistributionCalc();
		
		long availableUnitsOfWork = 0;
		List<Worker> workers = new ArrayList<>();
		workers.add(new MyWorker(10, Optional.of(13L)));
		workers.add(new MyWorker(25, Optional.of(50L)));
		workers.add(new MyWorker(12, Optional.of(13L)));
		workers.add(new MyWorker(5, Optional.of(50L)));

		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(0)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(1)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(2)));
		assertEquals(0L, calc.calculate(availableUnitsOfWork, workers, workers.get(3)));
	}
}
