package com.viktorx.skyblockbot.skyblock;

import net.minecraft.client.MinecraftClient;

public class SBPlayer {
    private String currentServer;
    private SBProfile profile;
    private SBGoal goal;

    public void run() {
        MinecraftClient client = MinecraftClient.getInstance();
        if(!SBUtils.isServerSkyblock()) {
            client.player.sendChatMessage("/l");
            client.player.sendChatMessage("/play sb");
        }
        profile.loadData();
    }

    public String getCurrentIsland() {
        //client.currentScreen.children();
        return "TODO";
    }
}
