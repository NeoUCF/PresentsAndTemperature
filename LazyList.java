import java.util.concurrent.locks.ReentrantLock;

// LazyList implementation directly from textbook
// [The Art of Multiprocessor Programming, 209-210]
public class LazyList {
	LLNode head;

	LazyList() {
		head = new LLNode();
	}

	private boolean validate(LLNode pred, LLNode curr) {
		return !pred.marked && !curr.marked && pred.next == curr;
	}

	public boolean add(Integer item) {
		if (item == null)
			return false;

		int key = item.hashCode();

		while (true) {
			LLNode pred = head;
			LLNode curr = head.next;

			while (curr.next != null && curr.key < key) {
				pred = curr;
				curr = curr.next;
			}

			pred.lock();

			try {
				curr.lock();

				try {
					if (validate(pred, curr)) {
						if (curr.key == key) {
							return false;
						} else {
							LLNode node = new LLNode(item);
							node.next = curr;
							pred.next = node;

							return true;
						}
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}

	public boolean remove(Integer item) {
		if (item == null)
			return false;

		int key = item.hashCode();

		while (true) {
			LLNode pred = head;
			LLNode curr = head.next;

			while (curr.next != null && curr.key < key) {
				pred = curr;
				curr = curr.next;
			}

			pred.lock();
			try {
				curr.lock();

				try {
					if (validate(pred, curr)) {
						if (curr.key != key) {
							return false;
						} else {
							curr.marked = true;
							pred.next = curr.next;

							return true;
						}
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}

	public boolean contains(Integer item) {
		if (item == null)
			return false;

		int key = item.hashCode();
		LLNode curr = head;

		while (curr.next != null && curr.key < key)
			curr = curr.next;

		return curr.key == key && !curr.marked;
	}
}

class LLNode {
	int tag;
	int key;
	LLNode next;
	boolean marked;
	ReentrantLock rel = new ReentrantLock(true); // true makes it fair

	LLNode() {
		this.tag = this.key = -2;
		this.next = new LLNode(-1);
	}

	LLNode(int tag) {
		this.tag = this.key = tag;
	}

	void lock() {
		rel.lock();
	}

	void unlock() {
		rel.unlock();
	}
}