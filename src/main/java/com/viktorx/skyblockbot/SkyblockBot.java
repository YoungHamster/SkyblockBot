package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.getInfo.GetComposterInfoExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.putItems.PutItemsInComposterExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItemExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem.BuyItemExecutor;
import com.viktorx.skyblockbot.task.base.changeIsland.ChangeIslandExecutor;
import com.viktorx.skyblockbot.task.base.pestKiller.PestKillerExecutor;
import com.viktorx.skyblockbot.task.base.replay.ReplayExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.sellSacks.SellSacksExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.useItem.UseItemExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems.GiveVisitorItemsExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor.TalkToVisitorExecutor;
import com.viktorx.skyblockbot.task.base.waitInQueue.WaitInQueueExecutor;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;
import com.viktorx.skyblockbot.utils.GlobalSettingsManager;
import com.viktorx.skyblockbot.utils.Utils;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.Level;
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
        WaitInQueueExecutor.INSTANCE.Init();
        PutItemsInComposterExecutor.INSTANCE.Init();
        GetComposterInfoExecutor.INSTANCE.Init();
        PestKillerExecutor.INSTANCE.Init();
    }
}
