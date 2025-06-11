package Coursework;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int corePoolSize = 2;
        int maxPoolSize = 4;
        long keepAliveTime = 5;
        TimeUnit unit = TimeUnit.SECONDS;
        int queueSize = 3;
        int minSpareThreads = 1;

        CustomExecutor pool = new CustomThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveTime, unit,
                queueSize, minSpareThreads,
                new CustomThreadFactory("MyPool"),
                (r, exec) -> System.out.println("[Rejected] Task " + r + " was rejected due to overload!")
        );

        for (int i = 1; i <= 8; i++) {
            final int id = i;
            Runnable work = () -> {
                System.out.println("[Task] " + Thread.currentThread().getName() + " started task #" + id);
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                System.out.println("[Task] " + Thread.currentThread().getName() + " finished task #" + id);
            };
            pool.execute(new NamedTask(id, work));
        }

        Thread.sleep(3000);
        System.out.println("[Main] Calling shutdown()");
        pool.shutdown();

        Thread.sleep(7000);

        System.out.println("\n[Main] Demonstrating shutdownNow()");
        CustomExecutor pool2 = new CustomThreadPoolExecutor(
                1, 2, keepAliveTime, unit,
                2, 1,
                new CustomThreadFactory("MyPool2"),
                (r, exec) -> System.out.println("[Rejected] (pool2) Task " + r + " rejected!")
        );

        for (int i = 1; i <= 4; i++) {
            final int id = i;
            Runnable work = () -> {
                System.out.println("[Task2] " + Thread.currentThread().getName() + " executing task2 #" + id);
                try { Thread.sleep(5000); } catch (InterruptedException e) {
                    System.out.println("[Task2] " + Thread.currentThread().getName() + " interrupted #" + id);
                }
            };
            pool2.execute(new NamedTask(id, work));
        }

        Thread.sleep(1000);
        System.out.println("[Main] Calling shutdownNow() on pool2");
        pool2.shutdownNow();
        System.out.println("[Main] shutdownNow() called. Pending tasks cleared in executor.");
    }
}

