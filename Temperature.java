import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Question:
// Do I have to worry too much about the whole
// "shared memory space" since I'm working in Java?

// This "shared memory space" is referring to a data structure? 

// Do we have to do this in a LinkedList or is that the aim of this problem?

// How many hours are being recorded? 3 or 4. can do 1
// Simulate 1 minute as 10 milliseconds?
public class Temperature {
	public static final int NUM_SENSORS = 8; // Number of threads
	public static final int NUM_HOURS = 1;
	public static final int MIN_IN_MS = 100; // A minute will be simulated as this # of milliseconds
	public static final int MIN_TEMP = -100;
	public static final int MAX_TEMP = 70;
	public LinkedBlockingDeque<Integer> tempReadings = new LinkedBlockingDeque<Integer>();
	public static Thread[] sensorThreads = new Thread[NUM_SENSORS];
	public static CyclicBarrier barrier = new CyclicBarrier(NUM_SENSORS);
	public static LockFreeList linkedList;
	public static LockFreeList diffList;
	public static int[] minuteReport = new int[NUM_SENSORS];
	public static int[] maxMinutesList;
	public static int[] minMinutesList;

	public static void main(String[] args) {
		timeAverage(1000);

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
			e.printStackTrace();
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
	public int maxAtMinute;
	public int minAtMinute;

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
		linkedList.firstAndLast5();
		diffList.getMax();
		System.out.println("==============================");
	}

	public void run() {
		for (int hour = 1; hour <= NUM_HOURS; hour++) {
			if (sensorThreads[0] == Thread.currentThread()) {
				linkedList = new LockFreeList();
				diffList = new LockFreeList();
				maxMinutesList = new int[60];
				minMinutesList = new int[60];
			}

			for (int minutes = 0; minutes < 60; minutes++) {
				int temp = ThreadLocalRandom.current().nextInt(MIN_TEMP, MAX_TEMP + 1);
				minuteReport[threadNum] = temp;
				barrierWait();

				for (int i = 0; i < NUM_SENSORS; i++) {
					maxMinutesList[minutes] = Math.max(minuteReport[i], maxMinutesList[minutes]);
					minMinutesList[minutes] = Math.min(minuteReport[i], minMinutesList[minutes]);
				}

				linkedList.add(temp);
				if (minutes >= 10) {
					int temp1 = maxMinutesList[minutes] - minMinutesList[minutes - 10];
					int temp2 = maxMinutesList[minutes - 10] - minMinutesList[minutes];

					diffList.add(Math.max(temp1, temp2), minutes + 1);
				}
			}

			if (sensorThreads[0] == Thread.currentThread()) {
				report(hour);
			}
		}

	}
}
