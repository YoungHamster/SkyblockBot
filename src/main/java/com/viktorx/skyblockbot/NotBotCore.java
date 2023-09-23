package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.movement.Bot;
import net.minecraft.client.network.ClientPlayerEntity;

public class NotBotCore {

    private static Bot botThread = null;
    public static boolean runBotThread = true;
    private static Thread t;

    public static void run(ClientPlayerEntity client) {
        if(botThread == null) {
            botThread = new Bot();
        }
        runBotThread = true;
        Thread t = new Thread(botThread);
        t.start();
    }

    public static void stop() {
        runBotThread = false;
    }
}
