import java.util.concurrent.locks.ReentrantLock;

// OptimisticList implementation directly from textbook
// [The Art of Multiprocessor Programming, 206-207]
public class OptimisticList {
	Node head;

	OptimisticList() {
		head  = new Node();
	}

	public boolean add(Integer tag) {
		if (tag == null)
			return false;

		// if (head.next == null)
		// {
		// 	head.next = new Node(tag); // !!!
		// }

		int key = tag;

		while (true) {
			Node pred = this.head;
			Node curr = pred.next;

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
						Node node = new Node(tag);
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
			Node pred = this.head;
			Node curr = pred.next;

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
			Node pred = this.head; // sentinel node
			Node curr = pred.next;

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

	private boolean validate(Node pred, Node curr) {
		Node node = this.head;

		while (node != null && node.key <= pred.key) {
			if (node == pred) {
				return pred.next == curr;
			}

			node = node.next;
		}

		return false;
	}
}

class Node {
	int tag;
	int key;
	Node next;
	ReentrantLock rel = new ReentrantLock(true); // true makes it fair

	Node() {
		this.tag = this.key = -2;
		this.next = new Node(-1);
		System.out.println("yoo");
	}

	Node(int tag) {
		this.tag = this.key = tag;
	}

	void lock() {
		rel.lock();
	}

	void unlock() {
		rel.unlock();
	}
}