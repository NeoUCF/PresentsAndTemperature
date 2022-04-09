import java.util.concurrent.atomic.AtomicMarkableReference;

// LockFreeList implementation directly from textbook
// [The Art of Multiprocessor Programming, 216-218]
public class LockFreeList {
	LFNode head;

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
				return true;
			}
		}
	}

	public boolean contains(Integer item) {
		// if (item == null)
		// return false;

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
}