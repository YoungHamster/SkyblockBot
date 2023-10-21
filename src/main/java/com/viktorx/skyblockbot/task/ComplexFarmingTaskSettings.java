package com.viktorx.skyblockbot.task;

public class ComplexFarmingTaskSettings {
    public static final long pauseInterval = 1000 * 60 * 120; // 120 mins
    public static final long pauseDuration = 1000 * 60 * 20; // 20 mins
    public static final long intervalBetweenRegularChecks = 1000 * 60 * 5; // 5 minutes
    public static final long godPotBuyThreshold = 1000 * 60 * 30; // 30 minutes
    public static final long cookieBuyThreshold = godPotBuyThreshold;
    public static final String gardenName = "Plot";
}
