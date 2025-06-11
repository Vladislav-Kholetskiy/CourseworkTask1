package Coursework;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable {
    private final BlockingQueue<Runnable> queue;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final CustomThreadPoolExecutor executor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread thread;

    public Worker(BlockingQueue<Runnable> queue,
                  long keepAliveTime,
                  TimeUnit timeUnit,
                  CustomThreadPoolExecutor executor) {
        this.queue = queue;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.executor = executor;
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        try {
            while (running.get() && !executor.isShutdown()) {
                Runnable task = queue.poll(keepAliveTime, timeUnit);
                if (task == null) {
                    if (executor.isRunningThreadsAboveCore()) {
                        System.out.println("[Worker] " + thread.getName() + " idle timeout, stopping.");
                        break;
                    }
                } else {
                    System.out.println("[Worker] " + thread.getName() + " executes " + task);
                    try {
                        task.run();
                    } catch (Exception e) {
                        System.out.println("[Worker] " + thread.getName() + " encountered exception: " + e.getMessage());
                    }
                }
            }
        } catch (InterruptedException ie) {
            // interrupted, exit
        } finally {
            System.out.println("[Worker] " + thread.getName() + " terminated.");
            executor.onWorkerExit(this);
        }
    }

    public void stop() {
        running.set(false);
        interruptIfIdle();
    }

    public void interruptIfIdle() {
        if (thread != null) {
            thread.interrupt();
        }
    }
}