import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Temperature {
	public static final int NUM_SENSORS = 8; // Number of threads
	public static final int NUM_HOURS = 3;
	public static final int MIN_IN_MS = 1000; // A minute will be simulated as this # of milliseconds
	public static final int MIN_TEMP = -100;
	public static final int MAX_TEMP = 70;
	public LinkedBlockingDeque<Integer> tempReadings = new LinkedBlockingDeque<Integer>();
	public static Thread[] sensorThreads = new Thread[NUM_SENSORS];
	public static CyclicBarrier barrier = new CyclicBarrier(NUM_SENSORS);
	public static int[] minuteReport = new int[NUM_SENSORS];

	public static void main(String[] args) {
		// timeAverage(100);

		setUpSensors();

		final long startTime = System.currentTimeMillis();
		recordTemp();
		final long endTime = System.currentTimeMillis();

		long executionTime = endTime - startTime;
		System.out.println(executionTime + " milliseconds to finish temperature readings.");
	}

	private static void timeAverage(int n) {
		long totalTime = 0;

		for (int i = 0; i < n; i++) {
			setUpSensors();

			final long startTime = System.currentTimeMillis();
			recordTemp();
			final long endTime = System.currentTimeMillis();
			totalTime += endTime - startTime;
		}

		System.out.println("Average Time: " + (totalTime / n) + "ms");
		System.out.println("==============================");
	}

	public static void setUpSensors() {
		Sensor tempHold;

		for (int i = 0; i < NUM_SENSORS; i++) {
			tempHold = new Sensor(i);

			sensorThreads[i] = new Thread(tempHold);
		}
	}

	public static void barrierWait() {
		try {
			barrier.await(MIN_IN_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
			// e.printStackTrace();
			System.err.println("A sensor was too slow at an iteration");
		}
	}

	public static void recordTemp() {
		System.out.println("Start Recording Temperature Process");
		System.out.println();

		// Begin Chaining Presents (start threads)
		for (int i = 0; i < NUM_SENSORS; i++)
			sensorThreads[i].start();

		try {
			// Wait for Threads to finish
			for (int i = 0; i < NUM_SENSORS; i++)
				sensorThreads[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class Sensor extends Temperature implements Runnable {
	public int threadNum;

	public Sensor(int th) {
		threadNum = th;
	}

	public void waitAMinute() {
		try {
			Thread.sleep(MIN_IN_MS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void report(int hour) {
		System.out.println("Report for hour " + hour + ":");
		SharedMemory.tempList.firstAndLast5();
		SharedMemory.reportList.getMaxDiff();
		System.out.println("==============================");
	}

	public void run() {
		for (int hour = 1; hour <= NUM_HOURS; hour++) {
			if (sensorThreads[0] == Thread.currentThread()) {
				SharedMemory.tempList = new LockFreeList();
				SharedMemory.reportList = new LockFreeReport();
			}

			for (int minutes = 0; minutes < 60; minutes++) {
				int temp = ThreadLocalRandom.current().nextInt(MIN_TEMP, MAX_TEMP + 1);
				minuteReport[threadNum] = temp;

				int maxAtMinute = Integer.MIN_VALUE;
				int minAtMinute = Integer.MAX_VALUE;
				barrierWait();

				for (int i = 0; i < NUM_SENSORS; i++) {
					maxAtMinute = Math.max(minuteReport[i], maxAtMinute);
					minAtMinute = Math.min(minuteReport[i], minAtMinute);
				}

				SharedMemory.tempList.add(temp);
				SharedMemory.reportList.add(minutes + 1, minuteReport, maxAtMinute, minAtMinute);
			}

			if (sensorThreads[0] == Thread.currentThread()) {
				report(hour);
			}
		}
	}
}

class SharedMemory {
	public static LockFreeList tempList;
	public static LockFreeReport reportList;
}
