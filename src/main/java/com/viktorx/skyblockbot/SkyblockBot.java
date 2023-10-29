package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.task.menuClickingTasks.buyBZItem.BuyBZItemExecutor;
import com.viktorx.skyblockbot.task.menuClickingTasks.buyItem.BuyItemExecutor;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandExecutor;
import com.viktorx.skyblockbot.task.replay.ReplayExecutor;
import com.viktorx.skyblockbot.task.menuClickingTasks.sellSacks.SellSacksExecutor;
import com.viktorx.skyblockbot.task.useItem.UseItemExecutor;
import com.viktorx.skyblockbot.task.menuClickingTasks.visitors.giveVisitorItems.GiveVisitorItemsExecutor;
import com.viktorx.skyblockbot.task.menuClickingTasks.visitors.talkToVisitor.TalkToVisitorExecutor;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SkyblockBot implements ModInitializer {

    public static final String MOD_ID = "skyblockbot";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello");
        try {
            GlobalSettingsManager.getInstance().loadSettings();
        } catch (IOException e) {
            SkyblockBot.LOGGER.error("Couldn't load settings!!!");
            MinecraftClient.getInstance().close();
        }

        Keybinds.Init();
        ReplayExecutor.INSTANCE.Init();
        ChangeIslandExecutor.INSTANCE.Init();
        BuyItemExecutor.INSTANCE.Init();
        SellSacksExecutor.INSTANCE.Init();
        BuyBZItemExecutor.INSTANCE.Init();
        UseItemExecutor.INSTANCE.Init();
        Utils.InitItemCounter();
        TGBotDaemon.INSTANCE.Init();
        TalkToVisitorExecutor.INSTANCE.Init();
        GiveVisitorItemsExecutor.INSTANCE.Init();
    }
}
