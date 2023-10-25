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

    protected abstract void restart();

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
