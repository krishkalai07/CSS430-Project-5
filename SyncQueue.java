
public class SyncQueue {
    private static class QueueNode {
        /**
         * A vector of child thread ids
         */
        private java.util.Vector<Integer> tidQueue = new java.util.Vector<>();
        
        public synchronized int sleep() {
            if (this. tidQueue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }

            return tidQueue.remove(0);
        }


        public synchronized void wake(int condition) {
            tidQueue.add(condition);
            notify();
        }

    }

    private QueueNode[] queue;

    public SyncQueue() {
        this(10);
    }

    public SyncQueue(int condMax) {
        this.queue = new QueueNode[condMax];

        for(int i = 0; i < condMax; i++) {
            this.queue[i] = new QueueNode();
        }
    }

    public int enqueueAndSleep(int condition) {
        if (condition < 0 || condition >= queue.length) {
            return -1;
        }
        return queue[condition].sleep();
    }


    public void dequeueAndWakeup(int condition) {
        this.dequeueAndWakeup(condition,0);
    }

    public void dequeueAndWakeup(int condition, int tid) {
        if (condition >= 0 && condition < this.queue.length) {
            this.queue[condition].wake(tid);
        }
    }
}
