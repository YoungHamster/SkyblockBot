package com.viktorx.skyblockbot;

import net.minecraft.item.ItemStack;

import java.util.List;

public class CurrentInventory {
    private static List<ItemStack> itemStacks = null;
    private static boolean syncIDChanged = false;
    private static int syncID = 0;

    public static void setItemStacks(List<ItemStack> itemStacks) {
        CurrentInventory.itemStacks = itemStacks;
    }

    public static List<ItemStack> getItemStacks() {
        return itemStacks;
    }

    public static boolean syncIDChanged() {
        SkyblockBot.LOGGER.info("SyncIdChanged was called");
        new Exception("e").printStackTrace();

        boolean returnValue = syncIDChanged;
        syncIDChanged = false;
        return returnValue;
    }

    public static void setSyncID(int syncID) {
        if (CurrentInventory.syncID != syncID) {
            syncIDChanged = true;
        }

        CurrentInventory.syncID = syncID;
    }

    public static int getSyncId() {
        return syncID;
    }
}
