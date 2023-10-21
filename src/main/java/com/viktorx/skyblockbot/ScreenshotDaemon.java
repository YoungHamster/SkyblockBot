package com.viktorx.skyblockbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import com.viktorx.skyblockbot.task.ComplexFarmingTask;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
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
    private int lastCarrotCount = 0;
    private int lastRedMushCount = 0;
    private int lastBrownMushCount = 0;
    private int lastCropieCount = 0;

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
                double carrotPrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.ENCH_CARROT.getName());
                double redMushPrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.ENCH_RED_MUSHROOM.getName());
                double brownMushPrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.ENCH_BROWN_MUSHROOM.getName());
                double cropiePrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.CROPIE.getName());

                int carrotCount = GlobalExecutorInfo.carrotCount.get();
                SkyblockBot.LOGGER.info("Carrot count: " + carrotCount);
                int redMushCount = GlobalExecutorInfo.redMushroomCount.get();
                int brownMushCount = GlobalExecutorInfo.brownMushroomCount.get();
                int cropieCount = GlobalExecutorInfo.cropieCount.get();

                carrotCount = carrotCount - lastCarrotCount;
                redMushCount = redMushCount - lastRedMushCount;
                brownMushCount = brownMushCount - lastBrownMushCount;
                cropieCount = cropieCount - lastCropieCount;

                lastCarrotCount = lastCarrotCount + carrotCount;
                lastRedMushCount = lastRedMushCount + redMushCount;
                lastBrownMushCount = lastBrownMushCount + brownMushCount;
                lastCropieCount = lastCropieCount + cropieCount;

                carrotCount = carrotCount / 160;
                redMushCount = redMushCount / 160;
                brownMushCount = brownMushCount / 160;

                int projectedProfit = (int)((carrotPrice * carrotCount + redMushPrice * redMushCount
                                            + brownMushCount * brownMushPrice + cropieCount * cropiePrice) * 12);

                String taskName;
                if(ComplexFarmingTask.INSTANCE.getCurrentTask() == null) {
                    taskName = "null. No task is currently executing";
                } else {
                    taskName = ComplexFarmingTask.INSTANCE.getCurrentTask().getClass().getName();
                    String[] foo = taskName.split("\\.");
                    taskName = foo[foo.length - 1];
                }

                takeAndSendScreenshot(
                        "Current task: " + taskName
                                + "\nEnchanted carrots picked up past 5 minutes: " + carrotCount
                                + "\nEnchanted red mushrooms picked up past 5 minutes: " + redMushCount
                                + "\nEnchanted brown mushrooms picked up past 5 minutes: " + brownMushCount
                                + "\nCropies picked up past 5 minutes: " + cropieCount
                                + "\nProjected 1h profit: " + projectedProfit,
                                false);
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
}
