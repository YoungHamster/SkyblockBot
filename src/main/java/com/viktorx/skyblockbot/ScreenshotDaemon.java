package com.viktorx.skyblockbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendPhoto;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenshotDaemon {
    public static final ScreenshotDaemon INSTANCE = new ScreenshotDaemon();

    private final Timer timer = new Timer(true);
    private final List<Integer> chatIds = new ArrayList<>();
    private boolean started = false;

    // 5 minutes
    private static final long delay = 1000 * 10;

    public void start() {
        if (!started) {
            started = true;
        } else {
            return;
        }

        SkyblockBot.LOGGER.info("Starting screenshotDaemon");
        // Create your bot passing the token received from @BotFather
        TelegramBot bot = new TelegramBot("531542929:AAFto3KT96LfWztnSYAlpNSok64d0BBr_qA");

        // Register for updates
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                chatIds.add(update.updateId());
            }
            // ... process updates
            // return id of last processed update or confirm them all
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
            // Create Exception Handler
        }, e -> {
            if (e.response() != null) {
                // got bad response from telegram
                e.response().errorCode();
                e.response().description();
            } else {
                // probably network error
                e.printStackTrace();
            }
        });
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SkyblockBot.LOGGER.info("Pressing F2 i guess");
                SkyblockBot.LOGGER.info("ChatIds size: " + chatIds.size());

                MinecraftClient.getInstance().options.screenshotKey.setPressed(true);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                for (Integer chatId : chatIds) {
                    bot.execute(new SendPhoto(chatId, Utils.getLastModified("screenshots")));
                }
            }
        }, 1, delay);
    }
}
