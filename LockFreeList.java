import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList {
	LFNode head;

	LockFreeList() {
		head = new LFNode();
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

					curr = succ;
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

			if (curr.next != null && curr.key == key) {
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

			if (curr.next != null && curr.key != key) {
				return false;
			} else {
				LFNode succ = curr.next.getReference();
				snip = curr.next.compareAndSet(succ, succ, false, true);

				if (!snip)
					continue;

				pred.next.compareAndSet(curr, succ, false, false);
				return true;
			}
		}
	}

	public boolean contains(Integer item) {
		if (item == null)
			return false;

		boolean[] marked = { false };
		int key = item.hashCode();
		LFNode curr = head;

		while (curr.next != null && curr.key < key) {
			curr = curr.next.getReference();
			LFNode succ = curr.next.get(marked);
		}

		return (curr.key == key && !marked[0]);
	}
}

class Window {
	public LFNode pred;
	public LFNode curr;

	Window(LFNode myPred, LFNode myCurr) {
		pred = myPred;
		curr = myCurr;
	}
}

class LFNode {
	int tag;
	int key;
	AtomicMarkableReference<LFNode> next;
	boolean marked;

	LFNode() {
		this.tag = this.key = -2;
		LFNode tempNext = new LFNode(-1);
		this.next = new AtomicMarkableReference<LFNode>(tempNext, false);
	}

	LFNode(int tag) {
		this.tag = this.key = tag;
	}
}