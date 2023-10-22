package com.viktorx.skyblockbot.task;

import net.minecraft.client.gui.screen.Screen;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalExecutorInfo {
    public static AtomicBoolean worldLoaded = new AtomicBoolean(false);
    public static AtomicBoolean worldLoading = new AtomicBoolean(false);

    public static final int waitTicksBeforeClick = 20;

    public static AtomicInteger carrotCount = new AtomicInteger(0);
    public static AtomicInteger redMushroomCount = new AtomicInteger(0);
    public static AtomicInteger brownMushroomCount = new AtomicInteger(0);
    public static AtomicInteger cropieCount = new AtomicInteger(0);
    public static AtomicInteger totalSackCount = new AtomicInteger(0);
    public static final int totalSackCountLimit = 50000; // When this limit is reached bot stops after loop and sells sacks
}
