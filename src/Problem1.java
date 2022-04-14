package src;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Note that our "un ordered" list here is ordered. In the eyes of the servants, the bag is still unordered.

public class Problem1 {
    final static int servantCount = 4;

    public static void main(String[] args) {
        LockFreeList list = new LockFreeList();
        ArrayList<Thread> threads = new ArrayList<Thread>();
        Servant runner = new Servant(list);

        // get all the servants going
        for (int i = 0; i < servantCount; i++) {
            Thread thread = new Thread(runner);
            threads.add(thread);
            thread.start();
        }

        // get the minotaur going
        Thread minotaur = new Thread(new Minotaur());
        threads.add(minotaur);
        minotaur.start();

        // wait for everyone to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("All " + Servant.numPresents + " guests got their thank you cards!");
    }

    public static class Servant implements Runnable {
        // the presents taken from the bag are put into this list
        LockFreeList list;
        public static final int numPresents = 500_000;
        static AtomicInteger currPresent = new AtomicInteger(),
                currCheck = new AtomicInteger();
        // sometimes, the minotaur will randomly set this to true
        // we check if it is before doing anything else
        // conceptually, a "random" thread will see this bool as true
        // and then set it to false
        static AtomicBoolean checkContains = new AtomicBoolean();

        public Servant(LockFreeList _list) {
            list = _list;
        }

        @Override
        public void run() {
            // whether the servent is currenly adding or removing from the list
            boolean isAdding = true;
            // once we have written thank you notes for each present, we are done
            while (currPresent.get() < numPresents) {
                // we are asked to check a present
                if (checkContains.getAndSet(false)) {
                    list.contains(currCheck.get());
                    continue;
                }
                if (isAdding) {
                    list.add(currPresent.getAndIncrement());
                } else {
                    int toDel = list.head.next.getReference().item;
                    if (toDel != Integer.MAX_VALUE) {
                        list.remove(toDel);
                    }
                }
                // switch between adding and removing
                isAdding = !isAdding;
            }
        }
    }

    // asks a servant to check for some random list element
    public static class Minotaur implements Runnable {
        static float chanceToAsk = .3f;

        @Override
        public void run() {
            while (Servant.currPresent.get() < Servant.numPresents) {
                if (ThreadLocalRandom.current().nextDouble(1) < chanceToAsk) {
                    Servant.currCheck.set(ThreadLocalRandom.current().nextInt(Servant.numPresents));
                    Servant.checkContains.set(true);
                }
                try {
                    // we dont need to try so often
                    TimeUnit.MILLISECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
