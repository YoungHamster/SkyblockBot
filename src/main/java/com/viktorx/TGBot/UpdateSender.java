package com.viktorx.TGBot;

import com.viktorx.skyblockbot.SkyblockBot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateSender {

    public static UpdateSender INSTANCE = new UpdateSender();
    private final Timer updateTimer = new Timer(true);
    private long lastUpdateTimestamp = 0;

    public void Init() {

        long firstDelay = 1000 * 60;
        long delay = 1000 * 60 * 2;

        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

            }
        }, firstDelay, delay);
    }

    public void takeAndSendScreenshot() {
        File screenshotFolder = new File("/screenshots");
        File[] files = screenshotFolder.listFiles();
        if (files == null) {
            SkyblockBot.LOGGER.warn("Screenshots folder is empty");
            return;
        }

        List<File> filesToSend = new ArrayList<>();
        for (File f : files) {
            if (Long.parseLong(f.getName().split("[_.]")[0]) > lastUpdateTimestamp) {
                filesToSend.add(f);
            }
        }
        lastUpdateTimestamp = System.currentTimeMillis();


    }
}
