package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.keybinds.KeybindsSettings;
import com.viktorx.skyblockbot.task.ComplexFarmingTaskSettings;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.buySellTask.BuySellSettings;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.ReplayBotSettings;
import com.viktorx.skyblockbot.tgBot.TGBotDaemonSettings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
            if(line.contains(": ")) {
                String[] tokens = line.split("[: ]");
                settings.put(tokens[0], tokens[2]);
            }
        });
        reader.close();

        KeybindsSettings.buttonPressDelay = Integer.parseInt(settings.get("Keybinds.buttonPressDelay"));

        BuySellSettings.waitTicksBetweenLetters = Integer.parseInt(settings.get("BuySell.waitTicksBetweenLetters"));

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

        ComplexFarmingTaskSettings.pauseInterval = Long.parseLong(settings.get("ComplexFarmingTask.pauseInterval"));
        ComplexFarmingTaskSettings.pauseDuration = Long.parseLong(settings.get("ComplexFarmingTask.pauseDuration"));
        ComplexFarmingTaskSettings.intervalBetweenRegularChecks = Long.parseLong(settings.get("ComplexFarmingTask.intervalBetweenRegularChecks"));
        ComplexFarmingTaskSettings.godPotBuyThreshold = Long.parseLong(settings.get("ComplexFarmingTask.godPotBuyThreshold"));
        ComplexFarmingTaskSettings.cookieBuyThreshold = Long.parseLong(settings.get("ComplexFarmingTask.cookieBuyThreshold"));
        ComplexFarmingTaskSettings.gardenName = settings.get("ComplexFarmingTask.gardenName");
        ComplexFarmingTaskSettings.retryGetOutOfLimboDelay = Long.parseLong(settings.get("ComplexFarmingTask.retryGetOutOfLimboDelay"));

        GlobalExecutorInfo.debugMode.set(Boolean.parseBoolean(settings.get("GlobalExecutor.debugMode")));
        GlobalExecutorInfo.waitTicksBeforeAction = Integer.parseInt(settings.get("GlobalExecutorInfo.waitTicksBeforeAction"));
        GlobalExecutorInfo.totalSackCountLimit = Integer.parseInt(settings.get("GlobalExecutorInfo.totalSackCountLimit"));

        TGBotDaemonSettings.firstDelay = Long.parseLong(settings.get("TGBotDaemon.firstDelay"));
        TGBotDaemonSettings.delayBetweenUpdates = Long.parseLong(settings.get("TGBotDaemon.delayBetweenUpdates"));
    }
}
