package com.viktorx.skyblockbot.task.replay;

public class ReplayBotSettings {

    public static String DEFAULT_RECORDING_FILE = "recording.bin";
    public static int antiDetectTriggeredWaitTicks = 30;
    public static int maxLagbackTicks = 50;
    public static int maxLagbackTicksWhenRecording = 20;
    public static double minDeltaToAdjust = 0.05d;
    public static double reactToLagbackThreshold = 0.75d;
    public static float antiDetectDeltaAngleThreshold = 15.0f;
    public static double maxDistanceToFirstPoint = 0.3f;
    public static int antiStucknessTickCount = 20;
    public static double detectStucknessCoefficient = 0.1;
    public static int checkForCollisionsAdvanceTicks = 4;
    public static boolean autoQuitWhenAntiDetect = true;

}
