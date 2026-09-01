package com.spellcaster.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ServerTaskQueue {

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    private ServerTaskQueue() {}

    public static void enqueue(Runnable task) {
        if (task != null) {
            TASKS.offer(task);
        }
    }

    public static void drain() {
        Runnable task;
        while ((task = TASKS.poll()) != null) {
            task.run();
        }
    }
}
