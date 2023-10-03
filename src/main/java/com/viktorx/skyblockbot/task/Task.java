package com.viktorx.skyblockbot.task;

public interface Task {

    void execute();
    void saveToFile(String filename);
    void completed();
    void aborted();
    void whenCompleted(Runnable runnable);
    void whenAborted(Runnable runnable);
}
