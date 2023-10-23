package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import com.viktorx.skyblockbot.task.ComplexFarmingTask;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ScreenshotDaemon {
    public static final ScreenshotDaemon INSTANCE = new ScreenshotDaemon();

    private final Timer timer = new Timer(true);
    private final long uid;
    private boolean started = false;

    private int lastTotalSackCount = 0;
    private int lastRedMushCount = 0;
    private int lastBrownMushCount = 0;
    private int lastCropieCount = 0;
    private final List<String> messageQueue = new ArrayList<>();
    private String botURL = "127.0.0.1";

    private ScreenshotDaemon() {
        Random random = new Random(System.currentTimeMillis());
        uid = random.nextLong();
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

                takeAndSendScreenshot(
                        "⛏Current task: " + taskName
                                + "\n\uD83E\uDD55: " + carrotCount
                                + "\n\uD83C\uDF44\uD83D\uDFE5: " + redMushCount
                                + "\n\uD83C\uDF44\uD83D\uDFEB: " + brownMushCount
                                + "\n\uD83D\uDFEB: " + cropieCount
                                + "\n\uD83D\uDCB0 1h: " + projectedProfit
                                + messages,
                        false);
            }
        }, firstDelay, delay);
    }

    public void takeAndSendScreenshot(String caption, boolean notify) {

        long timestamp = System.currentTimeMillis();
        String filename = "screenshots/" + timestamp + "_" + uid + ".";
        try {
            BufferedWriter file = new BufferedWriter(new FileWriter(filename + "txt"));
            file.write(caption);
            file.close();
        } catch (IOException e) {
            SkyblockBot.LOGGER.warn(filename + "txt");
            return;
        }

        byte[] screenshot;
        try {
            screenshot = takeScreenshot();
        } catch (IOException e) {
            SkyblockBot.LOGGER.warn("Couldn't take screenshot!!! IOException");
            return;
        }

        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("message", caption)
                .addBinaryBody("photo", screenshot)
                .build();

        HttpPost request = new HttpPost(botURL);
        request.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(request);

        // Waiting arbitrary amount of time because screenshots get taken asynchronously in minecraft
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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
