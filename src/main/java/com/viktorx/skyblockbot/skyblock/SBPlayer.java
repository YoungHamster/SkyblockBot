package com.viktorx.skyblockbot.skyblock;

import net.minecraft.client.MinecraftClient;

public class SBPlayer {
    private String currentServer;
    private SBProfile profile;
    private SBGoal goal;
    private MinecraftClient client;

    public void run() {
        client.player.sendChatMessage("/l");
        client.player.sendChatMessage("/play sb");

    }

    public String getCurrentIsland() {
        //client.currentScreen.children();
        return "TODO";
    }
}
