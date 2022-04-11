import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

// LockFreeReport implementation directly from textbook
// [The Art of Multiprocessor Programming, 216-218]
public class LockFreeReport {
	RNode head;
	AtomicInteger size = new AtomicInteger();

	LockFreeReport() {
		this.head = new RNode(Integer.MIN_VALUE);
		RNode tail = new RNode(Integer.MAX_VALUE);
		while (!head.next.compareAndSet(null, tail, false, false))
			;
	}

	public RWindow find(RNode head, int key) {
		RNode pred = null;
		RNode curr = null;
		RNode succ = null;
		boolean[] marked = { false };
		boolean snip;

		retry: while (true) {
			pred = head;
			curr = pred.next.getReference();

			while (true) {
				succ = curr.next.get(marked);
				while (marked[0]) {
					snip = pred.next.compareAndSet(curr, succ, false, false);

					if (!snip)
						continue retry;

					curr = pred.next.getReference();
					succ = curr.next.get(marked);
				}

				if (curr.key >= key)
					return new RWindow(pred, curr);

				pred = curr;
				curr = succ;
			}
		}
	}

	public boolean add(Integer item) {
		if (item == null)
			return false;

		int key = item.hashCode();

		while (true) {
			RWindow window = find(head, key);
			RNode pred = window.pred;
			RNode curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				RNode node = new RNode(item);
				node.next = new AtomicMarkableReference<RNode>(curr, false);

				if (pred.next.compareAndSet(curr, node, false, false)) {
					size.getAndIncrement();
					return true;
				}
			}
		}
	}

	public boolean add(Integer item, int key) {
		if (item == null)
			return false;

		while (true) {
			RWindow window = find(head, key);
			RNode pred = window.pred;
			RNode curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				RNode node = new RNode(item, key);
				node.next = new AtomicMarkableReference<RNode>(curr, false);

				if (pred.next.compareAndSet(curr, node, false, false)) {
					size.getAndIncrement();
					return true;
				}
			}
		}
	}

	public boolean add(Integer item, int[] minuteTemps, int max, int min) {
		if (item == null)
			return false;

		int key = item.hashCode();

		while (true) {
			RWindow window = find(head, key);
			RNode pred = window.pred;
			RNode curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				RNode node = new RNode(item, key, minuteTemps, max, min);
				node.next = new AtomicMarkableReference<RNode>(curr, false);

				if (pred.next.compareAndSet(curr, node, false, false)) {
					size.getAndIncrement();
					return true;
				}
			}
		}
	}

	public boolean remove(Integer item) {
		if (item == null)
			return false;

		int key = item.hashCode();
		boolean snip;

		while (true) {
			RWindow window = find(head, key);
			RNode pred = window.pred;
			RNode curr = window.curr;

			if (curr.key != key) {
				return false;
			} else {
				RNode succ = curr.next.getReference();
				snip = curr.next.compareAndSet(succ, succ, false, true); //

				if (!snip)
					continue;

				pred.next.compareAndSet(curr, succ, false, false);
				size.getAndDecrement();
				return true;
			}
		}
	}

	public boolean contains(Integer item) {
		if (item == null)
			return false;

		// boolean[] marked = { false };
		// int key = item.hashCode();
		// RNode curr = head;

		// while (curr.next != null && curr.key < key) {
		// curr = curr.next.getReference();
		// RNode succ = curr.next.get(marked);
		// }

		// return (curr.key == key && !marked[0]);
		int key = item.hashCode();
		RWindow window = find(head, key);
		RNode curr = window.curr;
		return (curr.key == key);
	}

	public LinkedBlockingDeque<Integer> getList() {
		LinkedBlockingDeque<Integer> tempDeque = new LinkedBlockingDeque<Integer>();
		RNode tempRNode = head.next.getReference();

		while (true) {
			if (tempRNode.minute == null)
				break;

			tempDeque.add(tempRNode.minute);
			tempRNode = tempRNode.next.getReference();
		}

		return tempDeque;
	}

	public void firstAndLast5() {
		RNode tempRNode = head.next.getReference();
		Stack<Integer> first = new Stack<Integer>();
		ArrayBlockingQueue<Integer> last = new ArrayBlockingQueue<Integer>(5);

		for (int i = 0; i < size.get(); i++) {
			if (tempRNode.minute == null)
				break;

			// Print first 5 in list
			if (i < 5)
				first.push(tempRNode.minute);
			if (i >= size.get() - 5)
				last.add(tempRNode.minute);

			tempRNode = tempRNode.next.getReference();
		}

		System.out.println("Top 5 Highest Temperature: " + last.toString());
		System.out.println("Top 5 Lowest Temperature: " + first.toString());
		System.out.println();
	}

	public int[] slidingWindowMax(int arr[], int n, int k) {
		int j, max;
		int[] slidingMax = new int[n - k + 1];

		for (int i = 0; i <= n - k; i++) {

			max = arr[i];

			for (j = 1; j < k; j++) {
				if (arr[i + j] > max)
					max = arr[i + j];
			}
			// System.out.print(max + " ");
			slidingMax[i] = max;
		}

		return slidingMax;
	}

	public int[] slidingWindowMin(int arr[], int n, int k) {
		int j, max;
		int[] slidingMin = new int[n - k + 1];

		for (int i = 0; i <= n - k; i++) {

			max = arr[i];

			for (j = 1; j < k; j++) {
				if (arr[i + j] < max)
					max = arr[i + j];
			}
			// System.out.print(min + " ");
			slidingMin[i] = max;
		}

		return slidingMin;
	}

	public void getMaxDiff() {
		RNode tempRNode = head.next.getReference();
		int maxDiff = Integer.MIN_VALUE;
		int[] maxList = new int[60];
		int[] minList = new int[60];
		int minuteIndex = -1;

		for (int i = 0; i < size.get(); i++) {
			if (tempRNode.minute == null)
				break;

			maxList[i] = tempRNode.max;
			minList[i] = tempRNode.min;
			tempRNode = tempRNode.next.getReference();
		}

		int[] windowedMax = slidingWindowMax(maxList, maxList.length, 10);
		int[] windowedMin = slidingWindowMin(minList, minList.length, 10);

		for (int i = 0; i < windowedMax.length; i++) {
			if (windowedMax[i] - windowedMin[i] > maxDiff) {
				maxDiff = windowedMax[i] - windowedMin[i];
				minuteIndex = i + 1;
			}
		}

		System.out.println("The largest temperature difference was " + maxDiff
				+ "F and it occured between minute " + minuteIndex
				+ " and minute " + (minuteIndex + 10));
		System.out.println();
	}
}

class RWindow {
	public RNode pred;
	public RNode curr;

	RWindow(RNode myPred, RNode myCurr) {
		this.pred = myPred;
		this.curr = myCurr;
	}
}

class RNode {
	Integer minute;
	int[] minuteReport;
	int max;
	int min;
	int key;
	AtomicMarkableReference<RNode> next;

	RNode(Integer item) {
		this.minute = item;
		this.key = item.hashCode();
		this.next = new AtomicMarkableReference<RNode>(null, false);
	}

	RNode(int key) {
		this.minute = null;
		this.key = key;
		this.next = new AtomicMarkableReference<RNode>(null, false);
	}

	RNode(Integer item, int key) {
		this.minute = item;
		this.key = key;
		this.next = new AtomicMarkableReference<RNode>(null, false);
	}

	RNode(Integer item, int key, int[] minuteTemp, int max, int min) {
		this.minute = item;
		this.key = key;
		this.minuteReport = minuteTemp;
		this.max = max;
		this.min = min;

		this.next = new AtomicMarkableReference<RNode>(null, false);
	}
}
