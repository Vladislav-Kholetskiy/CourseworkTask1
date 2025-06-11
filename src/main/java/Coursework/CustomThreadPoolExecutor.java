package Coursework;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueCapacity;
    private final int minSpareThreads;
    private final CustomThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectedHandler;

    private final List<Worker> workers = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();
    private final List<BlockingQueue<Runnable>> queues = new ArrayList<>();
    private final AtomicInteger rrCounter = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    public CustomThreadPoolExecutor(int corePoolSize,
                                    int maxPoolSize,
                                    long keepAliveTime,
                                    TimeUnit timeUnit,
                                    int queueCapacity,
                                    int minSpareThreads,
                                    CustomThreadFactory threadFactory,
                                    RejectedExecutionHandler rejectedHandler) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueCapacity = queueCapacity;
        this.minSpareThreads = minSpareThreads;
        this.threadFactory = threadFactory;
        this.rejectedHandler = rejectedHandler;

        for (int i = 0; i < maxPoolSize; i++) {
            queues.add(new LinkedBlockingQueue<>(queueCapacity));
        }
        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
    }

    private synchronized void addWorker() {
        if (threads.size() >= maxPoolSize) return;
        BlockingQueue<Runnable> queue = queues.get(threads.size());
        Worker worker = new Worker(queue, keepAliveTime, timeUnit, this);
        Thread thread = threadFactory.newThread(worker);
        workers.add(worker);
        threads.add(thread);
        thread.start();
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) throw new IllegalStateException("Executor is shutdown");
        int idx = Math.abs(rrCounter.getAndIncrement() % queues.size());
        BlockingQueue<Runnable> queue = queues.get(idx);

        System.out.println("[Pool] Task accepted into queue #" + idx + ": " + command);

        if (!queue.offer(command)) {
            if (threads.size() < maxPoolSize) {
                addWorker();
                queues.get(threads.size() - 1).offer(command);
            } else {
                rejectedHandler.rejectedExecution(command, null);
            }
        }
        ensureMinSpareWorkers();
    }

    @Override
    public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        execute(task);
        return task;
    }

    @Override
    public synchronized void shutdown() {
        isShutdown = true;
        // graceful: stop workers without interrupting main
        for (Worker w : workers) {
            w.stop();
        }
    }

    @Override
    public synchronized void shutdownNow() {
        isShutdown = true;
        for (Worker w : workers) {
            w.stop();
        }
    }

    void onWorkerExit(Worker w) {
        int idx = workers.indexOf(w);
        if (idx >= 0) {
            workers.remove(idx);
            threads.remove(idx);
        }
    }

    boolean isShutdown() {
        return isShutdown;
    }

    boolean isRunningThreadsAboveCore() {
        return threads.size() > corePoolSize;
    }

    private void ensureMinSpareWorkers() {
        int idleCount = threads.size() - activeCount();
        while (idleCount < minSpareThreads && threads.size() < maxPoolSize) {
            addWorker();
            idleCount++;
        }
    }

    private int activeCount() {
        int count = 0;
        for (Thread t : threads) {
            if (t.getState() == Thread.State.RUNNABLE) count++;
        }
        return count;
    }
}
