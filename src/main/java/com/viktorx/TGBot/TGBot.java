package com.viktorx.TGBot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.viktorx.skyblockbot.SkyblockBot;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;

public class TGBot {
    private static TelegramBot bot;
    private static final long chatId = 360813091;

    public static void main(String[] args) {
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

        UpdateSender.INSTANCE.Init();

    }

    public static long getChatId() {
        return chatId;
    }

    public static TelegramBot getBot() {
        return bot;
    }
}
