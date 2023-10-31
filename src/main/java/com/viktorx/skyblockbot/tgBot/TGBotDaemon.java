package com.viktorx.skyblockbot.tgBot;

import com.mojang.blaze3d.systems.RenderSystem;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import com.viktorx.skyblockbot.task.compound.FarmingTask;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import javafx.util.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class TGBotDaemon {
    public static final TGBotDaemon INSTANCE = new TGBotDaemon();

    private final Timer timer = new Timer(true);
    private final List<String> messageQueue = new ArrayList<>();
    private final String botURL = "http://127.0.0.1:8080";
    private boolean started = false;
    private int lastTotalSackCount = 0;
    private int lastRedMushCount = 0;
    private int lastBrownMushCount = 0;
    private int lastCropieCount = 0;

    public void Init() {
        start();
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
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                takeScreenshotAndSendInfo();
            }
        }, TGBotDaemonSettings.firstDelay, TGBotDaemonSettings.delayBetweenUpdates);
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

        float profitMultiplier = (1000f * 60f * 60f) / (float)TGBotDaemonSettings.delayBetweenUpdates;

        int projectedProfit = (int) (
                (carrotPrice * carrotCount + redMushPrice * redMushCount
                + brownMushCount * brownMushPrice + cropieCount * cropiePrice)
                * profitMultiplier);

        String taskName = FarmingTask.INSTANCE.getCurrentTaskName();

        List<String> messages;
        synchronized (messageQueue) {
            messages = new ArrayList<>(messageQueue);
            messageQueue.clear();
        }

        takeAndSendScreenshot(
                taskName,
                carrotCount,
                redMushCount,
                brownMushCount,
                cropieCount,
                projectedProfit,
                messages,
                false);
    }

    public void takeAndSendScreenshot(String simpleMsg, boolean notify) {
        List<Pair<String, String>> headers = new ArrayList<>();
        headers.add(new Pair<>("message", simpleMsg));
        takeAndSendScreenshot(headers, "/send_to_tg_simple", notify);
    }

    public void takeAndSendScreenshot(String taskName, int carrotCount, int redMushCount,
                                      int brownMushCount, int cropieCount, int projectedProfit,
                                      List<String> messages, boolean notify) {
        List<Pair<String, String>> headers = new ArrayList<>();
        headers.add(new Pair<>("Task", taskName));
        headers.add(new Pair<>("Carrot", Integer.toString(carrotCount)));
        headers.add(new Pair<>("RedMush", Integer.toString(redMushCount)));
        headers.add(new Pair<>("BrownMush", Integer.toString(brownMushCount)));
        headers.add(new Pair<>("Cropie", Integer.toString(cropieCount)));
        headers.add(new Pair<>("Profit", Integer.toString(projectedProfit)));
        for (int i = 0; i < messages.size(); i++) {
            headers.add(new Pair<>("Msg" + i, messages.get(i)));
        }
        takeAndSendScreenshot(headers, "/send_to_tg", notify);
    }

    private void takeAndSendScreenshot(List<Pair<String, String>> headers, String urlPath, boolean notify) {
        RenderSystem.recordRenderCall(() -> {
            byte[] screenshot;
            try {
                screenshot = takeScreenshot();
            } catch (IOException e) {
                SkyblockBot.LOGGER.warn("Couldn't take screenshot!!! IOException");
                return;
            }

            /*
             * Send screenshot asynchronously because otherwise we will use render thread and game will freeze for some time
             */
            CompletableFuture.runAsync(() -> {
                HttpPost request = new HttpPost(botURL + urlPath);

                for (Pair<String, String> header : headers) {
                    request.setHeader(header.getKey(), header.getValue());
                }
                request.setHeader("notify", Boolean.toString(notify));
                request.setEntity(EntityBuilder.create().setBinary(screenshot).build());

                HttpClient client = HttpClientBuilder.create().build();
                try {
                    String value = client.execute(request, ResponseHandler.INSTANCE);
                    SkyblockBot.LOGGER.info("Sent info to TGBot! Respone: " + value);
                } catch (IOException e) {
                    SkyblockBot.LOGGER.warn("Couldn't send info to TGBot for some reason");
                }
            });
        });
    }

    /*
     * Should only be called using RenderSystem.recordRenderCall()
     */
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
