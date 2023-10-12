package com.viktorx.skyblockbot.task;

public interface Task {

    void execute();
    void pause();
    void resume();
    void abort();
    void saveToFile(String filename);
    void completed();
    void aborted();
    void whenCompleted(Runnable runnable);
    void whenAborted(Runnable runnable);
    boolean isExecuting();
    boolean isPaused();
}
