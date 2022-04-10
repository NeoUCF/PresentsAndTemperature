import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Presents {
	public static final int NUM_PRESENTS = 500000;
	public static final int NUM_SERVANTS = 4;
	public static AtomicInteger numNotes = new AtomicInteger();
	public static ArrayBlockingQueue<Integer> bag;
	public static Thread[] servantThreads = new Thread[NUM_SERVANTS];
	public static CyclicBarrier barrier = new CyclicBarrier(NUM_SERVANTS);
	// public static OptimisticList linkedList = new OptimisticList();
	// public static LazyList linkedList = new LazyList();
	public static LockFreeList linkedList = new LockFreeList();

	public static void main(String[] args) {
		// timeAverage(100);

		setUpBag();
		setUpServants();

		final long startTime = System.currentTimeMillis();
		presentChain();
		final long endTime = System.currentTimeMillis();

		long executionTime = endTime - startTime;
		System.out.println(executionTime + " milliseconds to finish all Thank You Notes");
		System.out.println(numNotes.get() + " out of " + NUM_PRESENTS + " written.");
	}

	private static void timeAverage(int n)
    {
        long totalTime = 0;

        for (int i = 0; i < n; i++)
        {
			setUpBag();
			setUpServants();

            final long startTime = System.currentTimeMillis();
			presentChain();
            final long endTime = System.currentTimeMillis();
            totalTime += endTime - startTime;
        }

        System.out.println("Average Time: " + (totalTime / n) + "ms");
    }

	public static void setUpBag() {
		ArrayList<Integer> tempBag = new ArrayList<Integer>(NUM_PRESENTS);

		for (int i = 0; i < NUM_PRESENTS; i++)
			tempBag.add(i);

		Collections.shuffle(tempBag); // Make bag unordered

		bag = new ArrayBlockingQueue<Integer>(NUM_PRESENTS, false, tempBag);
	}

	public static void setUpServants() {
		Servant tempHold;

		for (int i = 0; i < NUM_SERVANTS; i++) {
			tempHold = new Servant();

			servantThreads[i] = new Thread(tempHold);
		}
	}

	public static void barrierWait() {
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public static void presentChain() {
		System.out.println("Start Chaining/Thank You Process");

		// Begin Chaining Presents (start threads)
		for (int i = 0; i < NUM_SERVANTS; i++)
			servantThreads[i].start();

		try {
			// Wait for Threads to finish
			for (int i = 0; i < NUM_SERVANTS; i++)
				servantThreads[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class Servant extends Presents implements Runnable {
	public void run() {
		barrierWait(); // Waits for all threads before continuing

		while (bag.peek() != null) {
			int action = ThreadLocalRandom.current().nextInt(0, 2); // values from 0-1
			int randCheck = ThreadLocalRandom.current().nextInt(0, NUM_PRESENTS);

			switch (action) {
				case 0:
					linkedList.contains(randCheck); // Check if Present is in chain
				default:
					// The alternating approach below was confirmed by Dylan to be fine.
					Integer giftBag = bag.poll();
					linkedList.add(giftBag); // Add present to chain
					if (linkedList.remove(giftBag)) // Remove present and write Thank You Note
						numNotes.getAndIncrement();
					break;
			}
		}
	}
}