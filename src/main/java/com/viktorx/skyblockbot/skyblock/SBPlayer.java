package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.skyblock.movementstuff.SBGoal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SBPlayer {
    private static final String SKYBLOCKBOT_TEST_INFO_PATH = "C:\\Users\\Nobody\\Desktop\\SBBotTestInfo.txt";
    private final SBProfile profile = new SBProfile();
    private String currentServer;
    private SBGoal goal;

    public void run() throws TimeoutException {

        if (!SBUtils.isServerSkyblock()) {
            Utils.sendChatMessage("/l");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Utils.sendChatMessage("/play sb");
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

        String sb = "Player data:\n" + "Current server: " + currentServer + "\n" + profile.toString();

        SkyblockBot.LOGGER.info(sb);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(SKYBLOCKBOT_TEST_INFO_PATH));
            writer.write(sb);

            writer.close();
        } catch (IOException ignored) {

        }
    }

    // looks at all available crafts and stores them for later processing(like looking for good ah and bz flips)
    public void collectCrafts() {

    }
}
