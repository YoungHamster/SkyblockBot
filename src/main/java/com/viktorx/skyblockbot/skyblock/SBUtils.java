package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.mixins.KeyBindingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SBUtils {

    // left clicks on inventory slot with itemStack that has said name
    public static void leftClickOnSlot(String itemStackName) throws TimeoutException {
        leftClickOnSlot(getSlot(itemStackName));
    }

    // left clicks on inventory slot with itemStack that has said name
    public static void leftClickOnSlot(Slot slot) throws TimeoutException {
        leftClickOnSlot(slot.id);
    }

    // left clicks on inventory slot with itemStack that has said name
    public static void leftClickOnSlot(int slotID) throws TimeoutException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        MinecraftClient client = MinecraftClient.getInstance();

        int lmb = ((KeyBindingMixin) client.options.attackKey).getBoundKey().getCode();
        client.interactionManager.clickSlot(CurrentInventory.getSyncId(), slotID, lmb, SlotActionType.PICKUP, client.player);
    }

    // gets profile balance from tab, sums personal bank balance and coop bank
    public static long getBankBalance() {
        String bankLine = getTabPlayers().stream().filter(string -> string.contains("Bank")).collect(Collectors.joining());
        if (bankLine.length() == 0) {
            return -1;
        }

        bankLine = bankLine.replace("Bank:", "");
        if (bankLine.contains("/")) {
            String[] personalAndCoop = bankLine.split("/");
            long coopBal = parseSBShortenedBalance(personalAndCoop[0]);
            long personalBal = parseSBShortenedBalance(personalAndCoop[1]);
            return coopBal + personalBal;
        } else {
            return parseSBShortenedBalance(bankLine);
        }
    }

    // takes 1B returns 1.000.000.000, same with 1M, 1k or 1
    // works with floats like 1.1M
    private static long parseSBShortenedBalance(String balance) {
        if (balance.contains("B")) {
            return ((long) Float.parseFloat(balance.replace("B", ""))) * 1000000000;
        } else if (balance.contains("M")) {
            return ((long) Float.parseFloat(balance.replace("M", ""))) * 1000000;
        } else if (balance.contains("k")) {
            return ((long) Float.parseFloat(balance.replace("k", ""))) * 1000;
        } else {
            return ((long) Float.parseFloat(balance));
        }
    }

    public static boolean isServerSkyblock() {
        return getIslandOrArea().length() == 0;
    }

    private static List<String> getTabPlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        Collection<PlayerListEntry> playerListEntries = client.player.networkHandler.getPlayerList();
        List<String> tabPlayers = new ArrayList<>();
        playerListEntries.forEach(entry -> {
            tabPlayers.add(client.inGameHud.getPlayerListHud().getPlayerName(entry).getString());
        });
        return tabPlayers;
    }

    // returns island or area that are displayed in sidebar(right scoreboard)
    public static String getIslandOrArea() {
        return ScoreboardUtils.
                getLines(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID)
                .stream().filter(string -> string.contains("⏣")).collect(Collectors.joining());
    }

    // returns purse value from sidebar(right scoreboard)
    public static long getPurse() {
        String purseLine = ScoreboardUtils.
                getLines(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID)
                .stream().filter(string -> string.contains("Purse")).collect(Collectors.joining());
        if (purseLine.length() == 0) {
            return -1;
        }
        return (long) Float.parseFloat(purseLine.replace("Purse:", ""));
    }

    public static String getSlotText(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player.currentScreenHandler.slots.get(slot).getStack().getName().getString();
    }

    // same as 'getAllSlots' but returns first of list of slots
    public static Slot getSlot(String itemStackName) throws TimeoutException {
        return getAllSlots(itemStackName).get(0);
    }

    // kind of heavy function for what it's doing, but lag-proofing should be very useful and you only need to do this once in while
    public static ItemStack getItemStack(int slotID) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player.currentScreenHandler.slots.get(slotID).getStack();
    }

    // returns all slots which have 'itemStackName' in their name
    // tries to get slot whose name contains that name(to account for text color) multiple times(to account for lag)
    // throws TimeoutException if doesn't find needed stack name after set amount of time
    public static List<Slot> getAllSlots(String itemStackName) throws TimeoutException {
        List<Slot> slotList;
        MinecraftClient client = MinecraftClient.getInstance();
        int numberOfTries = 100;
        int i = 0;
        do {
            slotList = client.player.currentScreenHandler.slots.stream()
                    .filter(slot -> slot.getStack().getName().getString().contains(itemStackName))
                    .collect(Collectors.toList());

            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            i++;
            if (i > numberOfTries) {
                throw new TimeoutException();
            }
        } while (slotList.size() == 0);
        return slotList;
    }

    public static boolean anySlotsWithName(String itemStackName) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player.currentScreenHandler.slots.stream()
                .filter(slot -> slot.getStack().getName().getString().contains(itemStackName)).count() > 0;
    }

    private static void waitForMenuToOpen() {
        while (!CurrentInventory.syncIDChanged()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        }
    }

    // waits up to 5 seconds or until item in lower left corner of supposed big chest is loaded
    private static void waitForMenuToLoad() throws TimeoutException {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack itemStack = null;
        int numberOfTries = 100;
        int i = 0;
        do {
            // basically waiting until last item(slot 53) loads to make sure everything loaded
            itemStack = client.player.currentScreenHandler.slots.get(53).getStack();

            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            i++;
            if (i > numberOfTries) {
                throw new TimeoutException();
            }
        } while (itemStack.getName().getString() == "Air");
    }

    public static void waitForMenu() throws TimeoutException {
        waitForMenuToOpen();
        waitForMenuToLoad();
    }
}
