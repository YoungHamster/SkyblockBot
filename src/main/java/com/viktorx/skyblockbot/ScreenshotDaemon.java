package com.viktorx.skyblockbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import com.viktorx.skyblockbot.task.ComplexFarmingTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenshotDaemon {
    public static final ScreenshotDaemon INSTANCE = new ScreenshotDaemon();

    private final Timer timer = new Timer(true);
    TelegramBot bot;
    private final long chatId = 360813091;
    private boolean started = false;

    // 5 minutes
    private final long delay = 1000 * 60 * 5;
    private final long firstDelay = 3000; // 3 secs
    private final String itemName = "Enchanted Carrot";
    private int sackCount = 0;

    public void start() {
        synchronized (this) {
            if (!started) {
                started = true;
            } else {
                return;
            }
        }

        SkyblockBot.LOGGER.info("Starting screenshotDaemon");
        // Create your bot passing the token received from @BotFather
        bot = new TelegramBot("531542929:AAEe3Ddw5OU38OvmOvxOEdgPd0dqxwzzIbM");

        // Register for updates
        bot.setUpdatesListener(updates -> {
            // ... process updates
            // return id of last processed update or confirm them all
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
            // Create Exception Handler
        }, e -> {
            if (e.response() != null) {
                // got bad response from telegram
                e.response().errorCode();
                SkyblockBot.LOGGER.info("Got bad response from telegram: " + e.response().description());
            } else {
                // probably network error
                e.printStackTrace();
            }
        });
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int count;
                synchronized (this) {
                    count = sackCount;
                }

                int projectedProfit = (int)(count
                        * PriceDatabase.getInstance().fetchItemPrice(itemName)
                        * 12);

                takeAndSendScreenshot(
                        "Current task: " + ComplexFarmingTask.INSTANCE.getCurrentTask().getClass().getName()
                                + "\nItems picked up past 5 minutes: " + count
                                + "\nProjected 1h profit: " + projectedProfit,
                                false);

                synchronized (this) {
                    sackCount = sackCount - count;
                }

            }
        }, firstDelay, delay);
    }

    public void takeAndSendScreenshot(String caption, boolean notify) {

        ScreenshotRecorder.saveScreenshot(
                new File(System.getProperty("user.dir")),
                MinecraftClient.getInstance().getFramebuffer(),
                text -> {
                });

        // Waiting arbitrary amount of time because screenshots get taken asynchronously in minecraft
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        File lastScreenshot = Utils.getLastModified("screenshots");
        if (!lastScreenshot.exists()) {
            SkyblockBot.LOGGER.info("Last screenshot doesn't exist for some reason");
            return;
        }

        SendPhoto info = new SendPhoto(chatId, lastScreenshot)
                .caption(caption).disableNotification(!notify);
        SendResponse sendResponse = bot.execute(info);

        if (sendResponse.isOk()) {
            SkyblockBot.LOGGER.info("Sent screenshot. Response ok: " + sendResponse.isOk());
        } else {
            SkyblockBot.LOGGER.info("Sent screenshot. Response ok: " + sendResponse.isOk()
                    + ", message: " + sendResponse.message());
        }
    }

    public void updateSackCount(int delta) {
        synchronized (this) {
            sackCount += delta;
        }
    }
}
