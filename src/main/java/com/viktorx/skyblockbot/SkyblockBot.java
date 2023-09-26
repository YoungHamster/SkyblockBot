package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.replay.ReplayBot;
import net.fabricmc.api.ModInitializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SkyblockBot implements ModInitializer {

    public static final String MOD_ID = "skyblockbot";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello");
        Keybinds.Init();
        ReplayBot.Init();
    }
}
