package com.viktorx.skyblockbot.task;

public interface Task {

    void execute();
    void saveToFile(String filename);
}
