package Coursework;

public class NamedTask implements Runnable {
    private final int id;
    private final Runnable delegate;

    public NamedTask(int id, Runnable delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        delegate.run();
    }

    @Override
    public String toString() {
        return "Task-" + id;
    }
}