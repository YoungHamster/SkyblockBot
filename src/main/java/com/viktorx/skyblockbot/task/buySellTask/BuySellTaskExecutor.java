package com.viktorx.skyblockbot.task.buySellTask;


import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.mixins.IChatHudMixin;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class BuySellTaskExecutor {
    private CompletableFuture<Boolean> currentClick;
    protected boolean currentClickRunning = false;
    protected int waitTickCounter = 0;
    private List<Integer> detectedStringsInChatIds = new ArrayList<>();

    protected abstract void restart();

    /**
     *
     * @param str self-explanatory
     * @param maxBacktrack how many messages it will check in chat, starting from the most recent one
     * @return true if recent messages in chat contain str, otherwise false
     */
    protected boolean isStringInRecentChat(String str, int maxBacktrack) {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        List<ChatHudLine<Text>> messages = ((IChatHudMixin) chat).getMessages();
        if (messages.size() == 0) {
            SkyblockBot.LOGGER.warn("BuyItem ERROR! The message history is empty, it's weird");
            return false;
        }

        int limit = Math.min(messages.size(), maxBacktrack);

        /*
         * Clearing out useless data so we don't leak memory when we run for long amounts of time
         */
        if(detectedStringsInChatIds.size() > messages.size()) {
            detectedStringsInChatIds = detectedStringsInChatIds.subList(0, messages.size());
        }

        for (int i = 0; i < limit; i++) {
            if(detectedStringsInChatIds.contains(messages.get(i).getId())) {
                continue;
            }
            if (messages.get(i).getText().getString().contains(str)) {
                detectedStringsInChatIds.add(messages.get(i).getId());
                return true;
            }
        }

        return false;
    }

    /*
     * Returns: true if done and clicked successfully, false if not done yet(or restarting)
     * If it restarts it just changes state to Restarting and onTick method goes with it
     */
    protected boolean asyncClickOrRestart(String itemName) {
        if (!currentClickRunning) {
            currentClick = CompletableFuture.supplyAsync(() -> waitAndClick(itemName));
            currentClickRunning = true;
        }

        if (!currentClick.isDone()) {
            return false;
        }
        currentClickRunning = false;

        try {
            if (!currentClick.get()) {
                restart();
                return false;
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }

        return true;
    }

    protected void asyncCloseCurrentInventory() {
        CompletableFuture.runAsync(this::blockingCloseCurrentInventory);
    }

    protected void blockingCloseCurrentInventory() {
        if (MinecraftClient.getInstance().currentScreen != null) {
            Keybinds.blockingPressKey(MinecraftClient.getInstance().options.inventoryKey);
        }
    }

    protected boolean waitAndClick(String slotName) {
        try {
            Thread.sleep(getTimeToWaitBeforeClick());
        } catch (InterruptedException ignored) {
        }

        try {
            SBUtils.leftClickOnSlot(slotName);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected long getTimeToWaitBeforeClick() {
        return GlobalExecutorInfo.waitTicksBeforeAction * 50;
    }

    protected boolean waitBeforeCommand() {
        if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
            return true;
        }
        waitTickCounter = 0;
        return false;
    }

}
