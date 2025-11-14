package com.vaultify.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void runAsync(Runnable task) {
        executor.submit(task);
    }
}
