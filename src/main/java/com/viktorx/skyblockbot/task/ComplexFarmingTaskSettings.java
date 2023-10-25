package com.viktorx.skyblockbot.task;

public class ComplexFarmingTaskSettings {
    public static final long pauseInterval = 1000 * 60 * 60; // 60 mins
    public static final long pauseDuration = 1000 * 60 * 10; // 10 mins
    public static final long intervalBetweenRegularChecks = 1000 * 60 * 5; // 5 minutes
    public static final long godPotBuyThreshold = 1000 * 60 * 30; // 30 minutes
    public static final long cookieBuyThreshold = godPotBuyThreshold;
    public static final String gardenName = "Plot";
    public static final long retryGetOutOfLimboDelay = 1000 * 60 * 10;
}
