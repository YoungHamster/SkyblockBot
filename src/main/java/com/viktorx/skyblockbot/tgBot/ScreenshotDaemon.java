package com.viktorx.skyblockbot.tgBot;

import com.mojang.blaze3d.systems.RenderSystem;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import com.viktorx.skyblockbot.task.ComplexFarmingTask;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenshotDaemon {
    public static final ScreenshotDaemon INSTANCE = new ScreenshotDaemon();
    private static final long timeBetweenConnectAttempts = 1000 * 20;

    private final Timer timer = new Timer(true);
    private long uid;
    private boolean started = false;

    private int lastTotalSackCount = 0;
    private int lastRedMushCount = 0;
    private int lastBrownMushCount = 0;
    private int lastCropieCount = 0;
    private final List<String> messageQueue = new ArrayList<>();
    private final String botURL = "http://127.0.0.1:8080";

    public void Init() {
        HttpGet request = new HttpGet(botURL + "/get_id");

        HttpClient client = HttpClientBuilder.create().build();
        try {
            uid = Long.parseLong(client.execute(request, ResponseHandler.INSTANCE));
            SkyblockBot.LOGGER.info("Got id from TGBot! uid: " + uid);
            start();
        } catch (IOException e) {
            SkyblockBot.LOGGER.warn("Couldn't get id from tg bot. Scheduling to retry. " + e.toString());

            Timer t = new Timer(true);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    ScreenshotDaemon.INSTANCE.Init();
                }
            }, timeBetweenConnectAttempts);

        }
    }

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
        // 3 secs
        long firstDelay = 3000;
        // 5 minutes
        long delay = 1000 * 60 * 5;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                takeScreenshotAndSendInfo();
            }
        }, firstDelay, delay);
    }

    private void takeScreenshotAndSendInfo() {
        double carrotPrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.ENCH_CARROT.getName());
        double redMushPrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.ENCH_RED_MUSHROOM.getName());
        double brownMushPrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.ENCH_BROWN_MUSHROOM.getName());
        double cropiePrice = PriceDatabase.getInstance().fetchItemPrice(ItemNames.CROPIE.getName());

        int totalCount = GlobalExecutorInfo.totalSackCount.get();
        int redMushCount = GlobalExecutorInfo.redMushroomCount.get();
        int brownMushCount = GlobalExecutorInfo.brownMushroomCount.get();
        int cropieCount = GlobalExecutorInfo.cropieCount.get();

        totalCount = totalCount - lastTotalSackCount;
        redMushCount = redMushCount - lastRedMushCount;
        brownMushCount = brownMushCount - lastBrownMushCount;
        cropieCount = cropieCount - lastCropieCount;


        lastTotalSackCount = lastTotalSackCount + totalCount;
        lastRedMushCount = lastRedMushCount + redMushCount;
        lastBrownMushCount = lastBrownMushCount + brownMushCount;
        lastCropieCount = lastCropieCount + cropieCount;

        redMushCount = redMushCount / 160;
        brownMushCount = brownMushCount / 160;
        int carrotCount = totalCount - redMushCount - brownMushCount - cropieCount;

        int projectedProfit = (int) ((carrotPrice * carrotCount + redMushPrice * redMushCount
                + brownMushCount * brownMushPrice + cropieCount * cropiePrice) * 12);

        String taskName = ComplexFarmingTask.INSTANCE.getCurrentTaskName();

        StringBuilder messages = new StringBuilder("\n");
        synchronized (messageQueue) {
            for (String message : messageQueue) {
                messages.append(message).append("\n");
            }
            messageQueue.clear();
        }

        /*takeAndSendScreenshot(
                "⛏Current task: " + taskName
                        + "\n\uD83E\uDD55: " + carrotCount
                        + "\n\uD83C\uDF44\uD83D\uDFE5: " + redMushCount
                        + "\n\uD83C\uDF44\uD83D\uDFEB: " + brownMushCount
                        + "\n\uD83D\uDFEB: " + cropieCount
                        + "\n\uD83D\uDCB0 1h: " + projectedProfit
                        + messages,
                false);*/
        takeAndSendScreenshot(
                "Current task on bot " + uid + ": " + taskName
                        + "\nCarrots: " + carrotCount
                        + "\nRed mush: " + redMushCount
                        + "\nBrown mush: " + brownMushCount
                        + "\nCropie: " + cropieCount
                        + "\nCoins 1h: " + projectedProfit
                        + messages,
                false);
    }

    public void takeAndSendScreenshot(String caption, boolean notify) {
        RenderSystem.recordRenderCall(() -> {
            byte[] screenshot;
            try {
                screenshot = takeScreenshot();
            } catch (IOException e) {
                SkyblockBot.LOGGER.warn("Couldn't take screenshot!!! IOException");
                return;
            }

            HttpPost request = new HttpPost(botURL + "/send_to_tg");
            request.setHeader("Content-Type", "charset=UTF-8");
            request.addHeader("message", caption);
            request.addHeader("notify", Boolean.toString(notify));
            request.setEntity(EntityBuilder.create().setBinary(screenshot).build());

            HttpClient client = HttpClientBuilder.create().build();
            try {
                String value = client.execute(request, ResponseHandler.INSTANCE);
                SkyblockBot.LOGGER.info("Sent info to TGBot! Respone: " + value);
            } catch (IOException e) {
                SkyblockBot.LOGGER.warn("Couldn't send info to TGBot for some reason");
            }
        });
    }

    private byte[] takeScreenshot() throws IOException {
        NativeImage image = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());
        return image.getBytes();
    }

    public void queueMessage(String message) {
        synchronized (messageQueue) {
            messageQueue.add(message);
        }
    }
}
