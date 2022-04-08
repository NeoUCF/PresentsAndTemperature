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

// For action 3, are we checking if a present is currently
// in the list, or if a present was in the list and is either
// in it or have been written a "Thank You"
public class Presents extends OptimisticList {
	public static final int NUM_PRESENTS = 50000;
	public static final int NUM_SERVANTS = 4;
	public static AtomicInteger numNotes = new AtomicInteger();
	public static ArrayBlockingQueue<Integer> bag;
	public static ArrayBlockingQueue<Integer> chain = new ArrayBlockingQueue<>(NUM_PRESENTS, true);
	public static Thread[] servantThreads = new Thread[NUM_SERVANTS];
	public static CyclicBarrier barrier = new CyclicBarrier(NUM_SERVANTS);
	// public static OptimisticList linkedList = new OptimisticList();
	public static LazyList linkedList = new LazyList();

	public static void main(String[] args) {

		setUpBag();
		setUpServants();

		final long startTime = System.currentTimeMillis();
		presentChain();
		final long endTime = System.currentTimeMillis();

		long executionTime = endTime - startTime;
		System.out.println(executionTime + " milliseconds to finish all Thank You Notes");
	}

	public static void setUpBag() {
		ArrayList<Integer> tempBag = new ArrayList<Integer>(NUM_PRESENTS);

		for (int i = 0; i < NUM_PRESENTS; i++)
			tempBag.add(i);

		Collections.shuffle(tempBag);

		bag = new ArrayBlockingQueue<Integer>(NUM_PRESENTS, true, tempBag);
	}

	public static void setUpServants() {
		if (NUM_SERVANTS < 1)
			return;

		Servant tempHold = new Servant();

		// Add other guests
		for (int i = 0; i < NUM_SERVANTS; i++) {
			tempHold = new Servant();
			// servants.add(tempHold);

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
		System.out.println("Waiting");
		// System.out.println(bag.toString());

		// Begin Chaining Presents (start threads)
		for (int i = 0; i < NUM_SERVANTS; i++) {
			servantThreads[i].start();
		}

		try {
			servantThreads[0].join(); // Wait for Threads to finish
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class Servant extends Presents implements Runnable {
	public void run() {
		barrierWait(); // Waits for all threads before continuing

		while (numNotes.get() != NUM_PRESENTS) {
			int action = ThreadLocalRandom.current().nextInt(0, 3); // values from 0-2
			int randCheck = ThreadLocalRandom.current().nextInt(0, NUM_PRESENTS);

			// System.out.println("Start: " + giftBag);
			
			// try {
			// System.out.println(bag.poll());
			// Thread.sleep(1000);
			// barrierWait();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }

			// chain.add(bag.poll());

			switch (action) {
				case 0:
					Integer giftBag = bag.poll();

					if (linkedList.add(giftBag))
					{
						// System.out.println("a" + Thread.currentThread().getName());

						// System.out.println("Adding: " + giftBag);

						chain.add(giftBag);
					}
					break;
				case 1:
					Integer giftChain = chain.peek();

					if (linkedList.remove(giftChain))
						if (chain.poll() != null)
						{
							numNotes.getAndIncrement();
							// System.out.println("Removed: " + giftChain);

							// System.out.println("h" + numNotes.getAndIncrement());
						}
					break;
				case 2:
				default:
					linkedList.contains(randCheck);
					break;

			}
			// System.out.println("p");
		}
	}
}