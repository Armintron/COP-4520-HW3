package src;

import java.util.concurrent.atomic.*;

// Implemented from the textbook
class LockFreeList {
    public Node head;

    public LockFreeList() {
        head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));
    }

    public boolean contains(int item) {
        boolean[] marked = { false };
        int key = ((Integer) item).hashCode();
        Node curr = head;
        while (curr.key < key) {
            curr = curr.next.getReference();
            curr.next.get(marked);
        }
        return (curr.key == key && !marked[0]);
    }

    public boolean add(int item) {
        int key = ((Integer) item).hashCode();
        while (true) {
            // what is this
            Window window = (this.new Window(head, head)).find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key == key) {
                return false;
            } else {
                Node node = new Node(item, curr);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(int item) {
        int key = ((Integer) item).hashCode();
        boolean snip;
        while (true) {
            Window window = (this.new Window(head, head)).find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key != key) {
                return false;
            } else {
                Node succ = curr.next.getReference();
                snip = curr.next.compareAndSet(succ, succ, false, true);
                if (!snip)
                    continue;
                pred.next.compareAndSet(curr, succ, false, false);
                return true;
            }
        }
    }

    public class Node {
        AtomicMarkableReference<Node> next;
        public int item;
        int key;

        public Node(int item, Node next) {
            this.next = new AtomicMarkableReference<Node>(next, false);
            this.item = item;
            this.key = ((Integer) item).hashCode();
        }
    }

    public class Window {
        public Node pred, curr;

        public Window(Node myPred, Node myCurr) {
            pred = myPred;
            curr = myCurr;
        }

        public Window find(Node head, int key) {
            Node pred = null, curr = null, succ = null;
            boolean[] marked = { false };
            boolean snip;
            while (true) {
                boolean breakOut = false;
                pred = head;
                curr = pred.next.getReference();
                while (true) {
                    succ = curr.next.get(marked);
                    while (marked[0]) {
                        snip = pred.next.compareAndSet(curr, succ, false, false);
                        if (!snip) {
                            breakOut = true;
                            break;
                        }
                        if (breakOut)
                            continue;
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
    }
}
