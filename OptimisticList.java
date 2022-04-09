import java.util.concurrent.locks.ReentrantLock;

// OptimisticList implementation directly from textbook
// [The Art of Multiprocessor Programming, 206-207]
public class OptimisticList {
	ONode head;

	OptimisticList() {
		head = new ONode();
	}

	public boolean add(Integer tag) {
		if (tag == null)
			return false;

		// if (head.next == null)
		// {
		// head.next = new ONode(tag); // !!!
		// }

		int key = tag;

		while (true) {
			ONode pred = this.head;
			ONode curr = pred.next;

			while (curr.next != null && curr.key < key) {
				pred = curr;
				curr = curr.next;
			}

			pred.lock();
			curr.lock();

			try {
				if (validate(pred, curr)) {
					if (curr.key == key) {
						return false;
					} else {
						ONode node = new ONode(tag);
						node.next = curr;
						pred.next = node;
						return true;
					}
				}
			} finally {
				pred.unlock();
				curr.unlock();
			}
		}
	}

	public boolean remove(Integer tag) {
		if (tag == null)
			return false;

		int key = tag;

		while (true) {
			ONode pred = this.head;
			ONode curr = pred.next;

			while (curr.next != null && curr.key < key) {
				pred = curr;
				curr = curr.next;
			}

			pred.lock();
			curr.lock();

			try {
				if (validate(pred, curr)) {
					if (curr.key == key) {
						pred.next = curr.next;
						return true;
					} else {
						return false;
					}
				}
			} finally {
				pred.unlock();
				curr.unlock();
			}
		}
	}

	public boolean contains(Integer tag) {
		if (tag == null)
			return false;

		int key = tag;

		while (true) {
			ONode pred = this.head; // sentinel node
			ONode curr = pred.next;

			while (curr.next != null && curr.key < key) {
				pred = curr;
				curr = curr.next;
			}

			pred.lock();
			curr.lock();

			try {
				if (validate(pred, curr)) {
					return (curr.key == key);
				}
			} finally { // always unlock
				pred.unlock();
				curr.unlock();
			}
		}
	}

	private boolean validate(ONode pred, ONode curr) {
		ONode node = this.head;

		while (node != null && node.key <= pred.key) {
			if (node == pred) {
				return pred.next == curr;
			}

			node = node.next;
		}

		return false;
	}
}

class ONode {
	int tag;
	int key;
	ONode next;
	ReentrantLock rel = new ReentrantLock(true); // true makes it fair

	ONode() {
		this.tag = this.key = -2;
		this.next = new ONode(-1);
		System.out.println("yoo");
	}

	ONode(int tag) {
		this.tag = this.key = tag;
	}

	void lock() {
		rel.lock();
	}

	void unlock() {
		rel.unlock();
	}
}