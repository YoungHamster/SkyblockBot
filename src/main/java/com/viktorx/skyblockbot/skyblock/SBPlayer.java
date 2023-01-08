package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.movementstuff.SBGoal;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SBPlayer {
    private String currentServer;
    private SBProfile profile = new SBProfile();
    private SBGoal goal;

    public void run() throws TimeoutException {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!SBUtils.isServerSkyblock()) {
            client.player.sendChatMessage("/l");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.player.sendChatMessage("/play sb");
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        profile.loadData();
    }

    public String getCurrentIsland() {
        //client.currentScreen.children();
        return "TODO";
    }

    public void TESTFUNCTION() throws TimeoutException {
        profile.loadUnlockedRecipes();
    }

    public void logPlayerInfo() {

        String sb = "Player data:\n" + "Current server: " + currentServer + "\n" +
                profile.toString();

        SkyblockBot.LOGGER.info(sb);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Nobody\\Desktop\\SBBotTestInfo.txt"));
            writer.write(sb);

            writer.close();
        } catch (IOException ignored) {

        }
    }

    // looks at all available crafts and stores them for later processing(like looking for good ah and bz flips)
    public void collectCrafts() {

    }
}
