package com.viktorx.skyblockbot.task;

import java.util.concurrent.atomic.AtomicInteger;

public class GlobalExecutorInfo {
    public static boolean worldLoaded = false;
    public static boolean worldLoading = false;

    public static final int waitTicksBeforeClick = 20;

    public static AtomicInteger totalSackCount = new AtomicInteger(0);
    public static final int totalSackCountLimit = 100000; // When this limit is reached bot stops after loop and sells sacks
}
