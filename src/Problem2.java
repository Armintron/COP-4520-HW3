/*
Report on efficiency, correctness, and progress:
    All concurrent operations are done without any locks or atomics. 
    Optimized Java data structures are used to store data. Thus, as a whole, this implementation is quite efficient. Correctness is ensured since threads are given a range of temperatures to choose from, and the shared object we use (ConcurrentSkipList) is thread safe. 
    The program is starvation and deadlock free as there are no dependencies between threads.
*/

package src;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

public class Problem2 {
    public final static int cpuCount = 8;
    public final static int minutes = 60;

    public static class Reading implements Comparable<Reading> {
        int minute = 0;
        int temp = 0;

        public Reading(int minute, int temp) {
            this.minute = minute;
            this.temp = temp;
        }

        @Override
        public int compareTo(Reading o) {
            return this.temp - o.temp;
        }

        @Override
        public String toString() {
            return "(" + minute + "," + temp + ")";
        }
    }

    public static void main(String[] args) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        // should only contain 10 minutes worth of readings
        ConcurrentSkipListSet<Reading> list = new ConcurrentSkipListSet<Reading>();
        SortedSet<Integer> lowest = new TreeSet<Integer>(), highest = new TreeSet<Integer>();
        int highestIntervalDifference = Integer.MIN_VALUE, highestIntervalStart = 0, highestIntervalEnd = 0;
        Function<Integer, Predicate<Reading>> toRemove = min -> x -> x.minute <= (min - 10);

        for (int minute = 1; minute <= minutes + 1; minute++) {
            // remove results that are over 10 minutes old
            list.removeIf(toRemove.apply(minute));
            // go get readings
            for (int cpu = 0; cpu < cpuCount; cpu++) {
                Thread thread = new Thread(new MyRunner(list, minute));
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            threads.clear();

            int lowestTemp = list.first().temp;
            int highestTemp = list.last().temp;
            // add lowest score if a new low
            if (lowest.size() < 5) {
                lowest.add(lowestTemp);
            } else if (lowest.first() > lowestTemp) {
                if (lowest.size() > 0)
                    lowest.remove(lowest.first());
                lowest.add(lowestTemp);
            }

            if (highest.size() < 5) {
                highest.add(highestTemp);
            } else if (highest.last() < highestTemp) {
                if (highest.size() > 0)
                    highest.remove(highest.last());
                // System.out.println("Adding " + highestTemp);
                highest.add(highestTemp);
            }

            // only look for a 10 minute interval if we have been over 10 minutes
            if (minute > 10) {
                int diff = list.last().temp - list.first().temp;
                if (diff > highestIntervalDifference) {
                    highestIntervalDifference = diff;
                    highestIntervalStart = minute - 10;
                    highestIntervalEnd = minute;
                }
            }
        }

        // generate report

        System.out.print("Lowest temps: ");
        for (Integer i : lowest) {
            System.out.print(i + ", ");
        }
        System.out.println();

        System.out.print("Highest temps: ");
        String temp = "";
        for (Integer i : highest) {
            temp = i + ", " + temp;
        }
        System.out.println(temp);

        System.out.println(
                "Biggest temp difference: " + highestIntervalDifference + " between minutes " + highestIntervalStart
                        + " and "
                        + highestIntervalEnd);
    }

    public static class MyRunner implements Runnable {
        public static final int lowestTemp = -100, highestTemp = 70;

        public ConcurrentSkipListSet<Reading> list;
        public int minute;

        public MyRunner(ConcurrentSkipListSet<Reading> list, int minute) {
            this.list = list;
            this.minute = minute;
        }

        @Override
        public void run() {
            list.add(new Reading(minute, ThreadLocalRandom.current().nextInt(lowestTemp, highestTemp + 1)));
        }

    }
}