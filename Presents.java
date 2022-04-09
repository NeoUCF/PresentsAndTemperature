import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// Questions:
// Can the 4 servants do this with alternating:
// S1:add(1) -> S1: remove(1) -> S1: add(4) -> ...
// S2:add(2) -> S2: remove(2) -> S2: add(5) -> ...
// S3:add(3) -> S3: remove(3) -> S3: add(6) -> ...
// S4:add(4) -> S4: remove(4) -> S4: add(7) -> ...
// Or must their add/remove/contains be truly random

// For action 3, are we checking if a present is currently
// in the list, or if a present was in the list and is either
// in it or have been written a "Thank You"

// 

// Expected/maximum runtime?
public class Presents {
	public static final int NUM_PRESENTS = 500000;
	public static final int NUM_SERVANTS = 4;
	public static AtomicInteger numNotes = new AtomicInteger();
	public static ArrayBlockingQueue<Integer> bag;
	public static Thread[] servantThreads = new Thread[NUM_SERVANTS];
	public static CyclicBarrier barrier = new CyclicBarrier(NUM_SERVANTS);
	public static OptimisticList linkedList = new OptimisticList();
	// public static LazyList linkedList = new LazyList();
	// public static LockFreeList linkedList = new LockFreeList();

	public static void main(String[] args) {

		setUpBag();
		setUpServants();

		final long startTime = System.currentTimeMillis();
		presentChain();
		final long endTime = System.currentTimeMillis();

		long executionTime = endTime - startTime;
		System.out.println(executionTime + " milliseconds to finish all Thank You Notes");
		System.out.println(numNotes.get() + " out of " + NUM_PRESENTS + " written.");
	}

	public static void setUpBag() {
		ArrayList<Integer> tempBag = new ArrayList<Integer>(NUM_PRESENTS);

		for (int i = 0; i < NUM_PRESENTS; i++)
			tempBag.add(i);

		Collections.shuffle(tempBag); // Make bag unordered

		bag = new ArrayBlockingQueue<Integer>(NUM_PRESENTS, true, tempBag);
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