import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

// LockFreeList implementation directly from textbook
// [The Art of Multiprocessor Programming, 216-218]
public class LockFreeList {
	LFNode head;
	AtomicInteger size = new AtomicInteger();

	LockFreeList() {
		this.head = new LFNode(Integer.MIN_VALUE);
		LFNode tail = new LFNode(Integer.MAX_VALUE);
		while (!head.next.compareAndSet(null, tail, false, false))
			;
	}

	public Window find(LFNode head, int key) {
		LFNode pred = null;
		LFNode curr = null;
		LFNode succ = null;
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
					return new Window(pred, curr);

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
			Window window = find(head, key);
			LFNode pred = window.pred;
			LFNode curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				LFNode node = new LFNode(item);
				node.next = new AtomicMarkableReference<LFNode>(curr, false);

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
			Window window = find(head, key);
			LFNode pred = window.pred;
			LFNode curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				LFNode node = new LFNode(item, key);
				node.next = new AtomicMarkableReference<LFNode>(curr, false);

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
			Window window = find(head, key);
			LFNode pred = window.pred;
			LFNode curr = window.curr;

			if (curr.key != key) {
				return false;
			} else {
				LFNode succ = curr.next.getReference();
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
		// LFNode curr = head;

		// while (curr.next != null && curr.key < key) {
		// curr = curr.next.getReference();
		// LFNode succ = curr.next.get(marked);
		// }

		// return (curr.key == key && !marked[0]);
		int key = item.hashCode();
		Window window = find(head, key);
		LFNode curr = window.curr;
		return (curr.key == key);
	}

	public LinkedBlockingDeque<Integer> getList() {
		LinkedBlockingDeque<Integer> tempDeque = new LinkedBlockingDeque<Integer>();
		LFNode tempLfNode = head.next.getReference();

		while (true) {
			if (tempLfNode.tag == null)
				break;

			tempDeque.add(tempLfNode.tag);
			tempLfNode = tempLfNode.next.getReference();
		}

		return tempDeque;
	}

	public void firstAndLast5() {
		LFNode tempLfNode = head.next.getReference();
		Stack<Integer> first = new Stack<Integer>();
		ArrayBlockingQueue<Integer> last = new ArrayBlockingQueue<Integer>(5);

		for (int i = 0; i < size.get(); i++) {
			if (tempLfNode.tag == null)
				break;

			// Print first 5 in list
			if (i < 5)
				first.push(tempLfNode.tag);
			if (i >= size.get() - 5)
				last.add(tempLfNode.tag);

			tempLfNode = tempLfNode.next.getReference();
		}

		System.out.println("Top 5 Highest Temperature: " + last.toString());
		System.out.println("Top 5 Lowest Temperature: " + first.toString());
		System.out.println();
	}

	public void getMax() {
		LFNode tempLfNode = head.next.getReference();
		int max = Integer.MIN_VALUE;
		int minuteIndex = -1;

		for (int i = 0; i < size.get(); i++) {
			if (tempLfNode.tag == null)
				break;

			if (tempLfNode.tag > max)
			{
				max = tempLfNode.tag;
				minuteIndex = tempLfNode.key;
			}

			tempLfNode = tempLfNode.next.getReference();
		}

		System.out.println("The largest temperature difference was " + max
							+ "F and it occured between minute " + (minuteIndex - 10)
							+ " and minute " + minuteIndex);
		System.out.println();
	}
}

class Window {
	public LFNode pred;
	public LFNode curr;

	Window(LFNode myPred, LFNode myCurr) {
		this.pred = myPred;
		this.curr = myCurr;
	}
}

class LFNode {
	Integer tag;
	int key;
	AtomicMarkableReference<LFNode> next;

	LFNode(Integer item) {
		this.tag = item;
		this.key = item.hashCode();
		this.next = new AtomicMarkableReference<LFNode>(null, false);
	}

	LFNode(int key) {
		this.tag = null;
		this.key = key;
		this.next = new AtomicMarkableReference<LFNode>(null, false);
	}

	LFNode(Integer item, int key) {
		this.tag = item;
		this.key = key;
		this.next = new AtomicMarkableReference<LFNode>(null, false);
	}
}