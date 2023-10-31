package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.keybinds.KeybindsSettings;
import com.viktorx.skyblockbot.task.compound.FarmingTaskSettings;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.BuySellSettings;
import com.viktorx.skyblockbot.task.base.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.base.replay.ReplayBotSettings;
import com.viktorx.skyblockbot.tgBot.TGBotDaemonSettings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GlobalSettingsManager {
    private static GlobalSettingsManager instance;

    public static synchronized GlobalSettingsManager getInstance() {
        if (instance == null) {
            instance = new GlobalSettingsManager();
        }
        return instance;
    }

    public void loadSettings() throws IOException {
        Map<String, String> settings = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader("settings.txt"));
        reader.lines().forEach(line -> {
            if (line.contains(": ")) {
                String[] tokens = line.split("[: ]");
                settings.put(tokens[0], tokens[2]);
            }
        });
        reader.close();

        KeybindsSettings.buttonPressDelay = Integer.parseInt(settings.get("Keybinds.buttonPressDelay"));

        BuySellSettings.maxWaitForScreen = Integer.parseInt(settings.get("BuySell.maxWaitForScreen"));

        ChangeIslandSettings.ticksToWaitForChunks = Integer.parseInt(settings.get("ChangeIsland.ticksToWaitForChunks"));
        ChangeIslandSettings.ticksToWaitBeforeAttempt = Integer.parseInt(settings.get("ChangeIsland.ticksToWaitBeforeAttempt"));
        ChangeIslandSettings.maxAttempts = Integer.parseInt(settings.get("ChangeIsland.maxAttempts"));

        ReplayBotSettings.DEFAULT_RECORDING_FILE = settings.get("ReplayBot.defaultRecordingFile");
        ReplayBotSettings.antiDetectTriggeredWaitTicks = Integer.parseInt(settings.get("ReplayBot.antiDetectTriggeredWaitTicks"));
        ReplayBotSettings.maxLagbackTicks = Integer.parseInt(settings.get("ReplayBot.maxLagbackTicks"));
        ReplayBotSettings.maxLagbackTicksWhenRecording = Integer.parseInt(settings.get("ReplayBot.maxLagbackTicksWhenRecording"));
        ReplayBotSettings.minDeltaToAdjust = Double.parseDouble(settings.get("ReplayBot.minDeltaToAdjust"));
        ReplayBotSettings.reactToLagbackThreshold = Double.parseDouble(settings.get("ReplayBot.reactToLagbackThreshold"));
        ReplayBotSettings.antiDetectDeltaAngleThreshold = Float.parseFloat(settings.get("ReplayBot.antiDetectDeltaAngleThreshold"));
        ReplayBotSettings.maxDistanceToFirstPoint = Float.parseFloat(settings.get("ReplayBot.maxDistanceToFirstPoint"));
        ReplayBotSettings.antiStucknessTickCount = Integer.parseInt(settings.get("ReplayBot.antiStucknessTickCount"));
        ReplayBotSettings.detectStucknessCoefficient = Double.parseDouble(settings.get("ReplayBot.detectStucknessCoefficient"));
        ReplayBotSettings.checkForCollisionsAdvanceTicks = Integer.parseInt(settings.get("ReplayBot.checkForCollisionsAdvanceTicks"));
        ReplayBotSettings.autoQuitWhenAntiDetect = Boolean.parseBoolean(settings.get("ReplayBot.autoQuitWhenAntiDetect"));
        ReplayBotSettings.maxTicksToWaitForSpawn = Integer.parseInt(settings.get("ReplayBot.maxTicksToWaitForSpawn"));

        FarmingTaskSettings.pauseInterval = Long.parseLong(settings.get("ComplexFarmingTask.pauseInterval"));
        FarmingTaskSettings.checkVisitorsInterval = Long.parseLong(settings.get("ComplexFarmingTask.checkGuestsInterval"));
        FarmingTaskSettings.pauseDuration = Long.parseLong(settings.get("ComplexFarmingTask.pauseDuration"));
        FarmingTaskSettings.intervalBetweenRegularChecks = Long.parseLong(settings.get("ComplexFarmingTask.intervalBetweenRegularChecks"));
        FarmingTaskSettings.godPotBuyThreshold = Long.parseLong(settings.get("ComplexFarmingTask.godPotBuyThreshold"));
        FarmingTaskSettings.cookieBuyThreshold = Long.parseLong(settings.get("ComplexFarmingTask.cookieBuyThreshold"));
        FarmingTaskSettings.gardenName = settings.get("ComplexFarmingTask.gardenName");
        FarmingTaskSettings.retryGetOutOfLimboDelay = Long.parseLong(settings.get("ComplexFarmingTask.retryGetOutOfLimboDelay"));

        GlobalExecutorInfo.debugMode.set(Boolean.parseBoolean(settings.get("GlobalExecutorInfo.debugMode")));
        GlobalExecutorInfo.waitTicksBeforeAction = Integer.parseInt(settings.get("GlobalExecutorInfo.waitTicksBeforeAction"));
        GlobalExecutorInfo.totalSackCountLimit = Integer.parseInt(settings.get("GlobalExecutorInfo.totalSackCountLimit"));

        TGBotDaemonSettings.firstDelay = Long.parseLong(settings.get("TGBotDaemon.firstDelay"));
        TGBotDaemonSettings.delayBetweenUpdates = Long.parseLong(settings.get("TGBotDaemon.delayBetweenUpdates"));
    }
}
