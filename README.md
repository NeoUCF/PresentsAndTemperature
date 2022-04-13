# PresentsAndTemperature
## How to Run the Program:
On the command prompt, go to the directory which contains the .java files.
For Problem 1 (Presents), run:
- `javac Presents.java`
- `java Presents`

For Problem 2 (Temperature), run:
- `javac Temperature.java`
- `java Temperature`

## Problem 1:
### Reasons the Servants' Initial Approach was Wrong
In the servants' initial approach where they had more presents than "Thank you" notes, they likely used the Optimistic Locking approach but they do not validate. If that occurs, there can be hanging nodes/presents where it can't be accessed from the head of the chain. For example, assume we have gifts with tags of 1, 2, 4, 5. If a servant tries to add 3 and another servant remove 2 without validating, it's possible that `HEAD -> 1 -> 4 -> 5` is the list, but `2 -> 3 -> 4 -> 5` exists where 2 and 3 are dangling from the list. So, 2 and 3 never gets accounted for when going through the chain. It's likely they also could have also used the Lock-Free List approach but they did not take into account that a logically removed present can still add a present to it as a successor (i.e. no markable bit). Hence there are presents that are not a part of the chain but never got written a Thank you note. Assume we have gifts ordered with tags of 1, 2, 3, 5. If a servant removes 3 and another servant adds 4 at the same time. It's possible that `HEAD -> 1 -> 2 -> 5` is the list, but `4 -> 5` also exists. So, 4 never gets accounted for when going through the chain.

### Correctness, Efficiency, and Evaluation:
To account for an unsorted bag, I used a collections class to shuffle it to get a random assortment. Then, I placed that assortment into an ArrayBlockingQueue since it handles concurrent enqueues and dequeues well.

The way I handle the servants actions is by doing an alternation between adding a present and removing a present with a random chance of checking if the chain contains a present. After successfully removing a present from the chain, a counter is incremented to keep track the number of Thank You Notes. The servants continues these operations until the bag has no items to dequeue.

I used the LockFreeList approach. Its **Correctness** is ensured by the markable bits associated with each of the node/presents which determines if it has been logically deleted and if can be performed with an add/delete operation (In OptimisticList and LazyList Correctness is ensured by the validate method which allows it to check the existence of a node/present). The use of logical deletion before a physical deletion ensures there will not be a dangling node/present on the chain where the head of the chain can't access it. (In OptimisticList and LazyList, the Correctness is also ensured by the use of two locks. By having the two locks, it ensures there will not be a dangling node/present on the chain where the head of the chain can't access it. Although the locks can slow down the program a bit, it guarantees freedom from interference.)

In terms of **Efficiency**, this LockFreeList works well with many threads doing concurrent adds, removes, and contains. contains can be randomly called, but it will still perform add and remove after. Given that the real-time ordering is unpredictable the order of which one of these methods can be considered pseudo-random. However, since I made the add and remove methods called on every iteration, and the iterations continue until the bag (ArrayBlockingQueue) is empty, then the there should be n iterations (where n is the number of presents in the bag). So the runtime is O(n). The multi-threaded approach is slower than the single-thread thread approach because of the resources handling the balance of work among the threads running.

For **Experimental Evaluation**, I tested between 3 different approaches for concurrent linked list: OptimisticList, LazyList, and LockFreeList. The implementations come from the textbook. After testing runtimes between the three approaches, I've found that LockFreeList generally had a faster runtime compared to the other two based on how I structured my program. Running the LockFreeList with 4 servants and 500000 presents on 100 runs would take an average of 159 milliseconds per run. Running the OptimisticList with 4 servants and 500000 presents on 100 runs would take an average of 2987 milliseconds per run. Running the LazyList with 4 servants and 500000 presents on 100 runs would take an average of 13671 milliseconds per run. All the experimental tests take place on a Lenovo IdeaPad Flex 5 which has a series 4000 Ryzen 7 (8 core CPU). You can test these other implementations by switching lines 16-18 on Presents.java

## Problem 2:
The sensors (threads) utilize a LockFree Linked List to store the temperature readings in a shared memory space. The add/remove/contains is the same implementation as the one used in problem 1. I use the LockFreeList class to get a record of all the temperatures recorded in sorted order (no duplicates). This is used in the hourly report to get the top 5 highest and lowest (unique) temperatures for that hour. I also use a slightly altered version of the linked list in the LockFreeReport class to keep a record of all temperatures by each sensor for each minute. This class also includes a method to calculate all the largest difference within a 10-minute interval.

### Correctness, Efficiency, and Evaluation:
The **Correctness** of the program relies on the lock free linked list. This type of list works by the use of markable bits and it doesn't allow nodes with duplicate key values. So, my LockFreeReport has a list of nodes with minutes as the key. My LockFreeList is a list of nodes with temperature as the key. The problem also specifies that readings occur in minute intervals. So I use a CyclicBarrier to have the threads wait for each other before continuing. The barrier also has timeout length before the threads waiting have to record the temperature so they don't miss the interval (Since the threads in my program work fast, this likely will not occur). Otherwise, if all the threads make it to the barrier before the timeout, then we assume the threads have waited that interval length to do the next temperature recording. In general, this barrier helps keep the threads on the same minute interval without a thread being behind or ahead. When an hour of measuring is complete, the program prints a report for that hour. If there are more hours to report on, we do the same process as before, but we wipe the linked list object with another new linked list to add to.

As described in the correctness in problem 1, the lists does logical deletion when doing a remove. However, in this program, I only use the add method and I don't use the remove method. So the **Efficiency** of this program is very good when adding since there is no locks. It is also known that since there is 60 minutes (m), we are storing 60 nodes in LockFreeReport. So it has a runtime of O(nm) where n is the max length of the list. In this problem, we know there are 60 minutes and 8 threads, so we expect 480 attempts at adding to the list. At the end of the hour, a thread attempts to print a report and this consists of going through the LockFreeList once (to get the top 5 highest and lowest temperatures) and going through LockFreeReport once to get the largest difference within a 10-minute interval. Getting the top 5 is O(n) while getting the k-minute difference is O(nk).

For **Experimental Evaluation**, running 8 sensors in 1 hour of temperature recording on 100 runs would take an average of 23 milliseconds. All the experimental tests take place on a Lenovo IdeaPad Flex 5 which has a series 4000 Ryzen 7 (8 core CPU).

Overall, the progress guarantee of this program is that it is lock free since the program uses the lock free implementation of the linked list. This means there is always some call that completes in a finite number of steps.

## Problem 1: The Birthday Presents Party (50 points)

The Minotaur’s birthday party was a success. The Minotaur received a lot of presents from his guests. The next day he decided to sort all of his presents and start writing “Thank you” cards. Every present had a tag with a unique number that was associated with the guest who gave it. Initially all of the presents were thrown into a large bag with no particular order. The Minotaur wanted to take the presents from this unordered bag and create a chain of presents hooked to each other with special links (similar to storing elements in a linked-list). In this chain (linked-list) all of the presents had to be ordered according to their tag numbers in increasing order. The Minotaur asked 4 of his servants to help him with creating the chain of presents and writing the cards to his guests. Each servant would do one of three actions in no particular order:

   1) Take a present from the unordered bag and add it to the chain in the correct location by hooking it to the predecessor’s link. The servant also had to make sure that the newly added present is also linked with the next present in the chain.
   2) Write a “Thank you” card to a guest and remove the present from the chain. To do so, a servant had to unlink the gift from its predecessor and make sure to connect the predecessor’s link with the next gift in the chain.
   3) Per the Minotaur’s request, check whether a gift with a particular tag was present in the chain or not; without adding or removing a new gift, a servant would scan through the chain and check whether a gift with a particular tag is already added to the ordered chain of gifts or not.

As the Minotaur was impatient to get this task done quickly, he instructed his servants not to wait until all of the presents from the unordered bag are placed in the chain of linked and ordered presents. Instead, every servant was asked to alternate adding gifts to the ordered chain and writing “Thank you” cards. The servants were asked not to stop or even take a break until the task of writing cards to all of the Minotaur’s guests was complete.

After spending an entire day on this task the bag of unordered presents and the chain of ordered presents were both finally empty!

Unfortunately, the servants realized at the end of the day that they had more presents than “Thank you” notes. What could have gone wrong?

Can we help the Minotaur and his servants improve their strategy for writing “Thank you” notes?

Design and implement a concurrent linked-list that can help the Minotaur’s 4 servants with this task. In your test, simulate this concurrent “Thank you” card writing scenario by dedicating 1 thread per servant and assuming that the Minotaur received 500,000 presents from his guests.

 

## Problem 2: Atmospheric Temperature Reading Module (50 points)

You are tasked with the design of the module responsible for measuring the atmospheric temperature of the next generation Mars Rover, equipped with a multi-core CPU and 8 temperature sensors. The sensors are responsible for collecting temperature readings at regular intervals and storing them in shared memory space. The atmospheric temperature module has to compile a report at the end of every hour, comprising the top 5 highest temperatures recorded for that hour, the top 5 lowest temperatures recorded for that hour, and the 10-minute interval of time when the largest temperature difference was observed. The data storage and retrieval of the shared memory region must be carefully handled, as we do not want to delay a sensor and miss the interval of time when it is supposed to conduct temperature reading. 

Design and implement a solution using 8 threads that will offer a solution for this task. Assume that the temperature readings are taken every 1 minute. In your solution, simulate the operation of the temperature reading sensor by generating a random number from -100F to 70F at every reading. In your report, discuss the efficiency, correctness, and progress guarantee of your program.
