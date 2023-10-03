package com.viktorx.skyblockbot.task.replay;

public class ReplayBotSettings {

    public static final String DEFAULT_RECORDING_FILE = "recording.bin";
    public static final int antiDetectTriggeredWaitTicks = 30;
    public static final int maxLagbackTicks = 10;
    public static final int maxLagbackTicksWhenRecording = 20;
    public static final double minDeltaToAdjust = 0.05d;
    public static final double reactToLagbackThreshold = 0.75d;
    public static final float antiDetectDeltaAngleThreshold = 15.0f;
    public static final double maxDistanceToFirstPoint = 0.3f;
    public static final int antiStucknessTickCount = 20;
    public static final double detectStucknessCoefficient = 0.1;
    public static final int checkForCollisionsAdvanceTicks = 4;

}
