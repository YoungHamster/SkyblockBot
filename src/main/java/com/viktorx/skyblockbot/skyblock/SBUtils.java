package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.utils.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.PlayerListHudMixin;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SBUtils {

    private static final long second = 1000;
    private static final long minute = second * 60;
    private static final long hour = minute * 60;
    private static final long day = hour * 24;

    // left clicks on inventory slot with itemStack that has said name
    public static void leftClickOnSlot(String itemStackName) throws TimeoutException {
        leftClickOnSlot(getSlot(itemStackName));
    }

    // left clicks on inventory slot
    public static void leftClickOnSlot(Slot slot) throws TimeoutException {
        leftClickOnSlot(slot.id);
    }

    // left clicks on inventory slot with slotID
    public static void leftClickOnSlot(int slotID) throws TimeoutException {
        MinecraftClient client = MinecraftClient.getInstance();

        /*
         * Hardcoding left mouse button because it will never change and it'll work fine
         * Actually the previous method i used here didn't work when you change attack key in settings,
         * because inventory doesn't care about settings, it only cares about default buttons
         */
        int lmb = 0;
        assert client.interactionManager != null;
        client.interactionManager.clickSlot(CurrentInventory.getSyncId(), slotID, lmb, SlotActionType.PICKUP, client.player);
    }

    public static void quickSwapSlotWithHotbar(int slotID, int hotbar) {
        MinecraftClient client = MinecraftClient.getInstance();

        assert client.interactionManager != null;
        client.interactionManager.clickSlot(CurrentInventory.getSyncId(), slotID, hotbar, SlotActionType.SWAP, client.player);
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

    public static int getGardenVisitorCount() {
        String visitorLine = getTabPlayers().stream().filter(string -> string.contains("Visitors")).collect(Collectors.joining());

        if (visitorLine.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(visitorLine.substring(11, 12));
        }
    }

    public static boolean isGardenVisitorInQueue(String name) {
        String visitorLine = getTabPlayers().stream().filter(string -> string.contains(name)).collect(Collectors.joining());
        return visitorLine.length() == 0;
    }

    /*
     * God pot time is written with capital letter
     */
    public static long getTimeLeftGodPot() {
        PlayerListHudMixin hud = (PlayerListHudMixin) MinecraftClient.getInstance().inGameHud.getPlayerListHud();
        String[] footer = hud.getFooter().getString().split("\n");
        String godPotTime = null;
        for (String line : footer) {
            if (line.contains("God Potion")) {
                godPotTime = line.split("active! ")[1];
                break;
            }
        }
        if (godPotTime == null) {
            return 0;
        }

        String time = godPotTime.split(" ")[0];

        if (godPotTime.contains("Hours") || godPotTime.contains("Hour")) {
            return Long.parseLong(time) * hour;
        } else if (godPotTime.contains("Minutes")) {
            return Long.parseLong(time) * minute;
        } else {
            if (time.equals("1h")) {
                return hour;
            }
            return Long.parseLong(time) * second;
        }
    }

    /*
     * Cookie buff time is written with regular letter
     */
    public static long getTimeLeftCookieBuff() {
        PlayerListHudMixin hud = (PlayerListHudMixin) MinecraftClient.getInstance().inGameHud.getPlayerListHud();
        String[] footer = hud.getFooter().getString().split("\n");
        String cookieTime = null;
        for (int i = 0; i < footer.length; i++) {
            if (footer[i].contains("Cookie Buff")) {
                cookieTime = footer[i + 1];
                break;
            }
        }
        if (cookieTime == null) {
            return 0;
        }

        if (cookieTime.contains("Less than an hour")) {
            return 0;
        } else if (cookieTime.contains("days") || cookieTime.contains("day")) {
            return Long.parseLong(cookieTime.split(" ")[0]) * day;
        } else if (cookieTime.contains("hours") || cookieTime.contains("hour")) {
            return Long.parseLong(cookieTime.split(" ")[0]) * hour;
        } else if (cookieTime.contains("minutes")) {
            return Long.parseLong(cookieTime.split(" ")[0]) * minute;
        } else {
            return Long.parseLong(cookieTime.split(" ")[0]) * second;
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
        return getIslandOrArea().length() != 0;
    }

    private static List<String> getTabPlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
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
                getLines(ScoreboardDisplaySlot.SIDEBAR)
                .stream().filter(string -> string.contains("⏣")).collect(Collectors.joining());
    }

    // returns purse value from sidebar(right scoreboard)
    public static long getPurse() {
        String purseLine = ScoreboardUtils.
                getLines(ScoreboardDisplaySlot.SIDEBAR)
                .stream().filter(string -> string.contains("Purse")).collect(Collectors.joining());

        if (purseLine.length() == 0) {
            return -1;
        }

        purseLine = purseLine.replace(",", ""); // erase , between triplets
        return (long) Float.parseFloat(purseLine.replace("Purse:", ""));
    }

    public static String getSlotText(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();

        assert client.player != null;
        return client.player.currentScreenHandler.slots.get(slot).getStack().getName().getString();
    }

    public static List<String> getSlotLore(String itemName) throws TimeoutException {
        NbtCompound nbt = getSlot(itemName).getStack().getNbt();
        if (nbt == null) {
            SkyblockBot.LOGGER.info("Nbt is null! " + getSlot(itemName).id);
            return null;
        }

        nbt = nbt.getCompound("display");
        if (nbt == null) {
            SkyblockBot.LOGGER.info("display is null! " + getSlot(itemName).id);
            return null;
        }

        List<String> lines = new ArrayList<>();
        nbt.getList("Lore", NbtElement.STRING_TYPE).forEach(elem -> {
            String str = elem.asString();
            String token = "text\":\"";
            StringBuilder loreLine = new StringBuilder();
            while (str.contains(token)) {
                str = str.substring(str.indexOf(token) + token.length());

                loreLine.append(str, 0, str.indexOf("\""));
            }
            lines.add(loreLine.toString());
        });

        return lines;
    }

    // same as 'getAllSlots' but returns first of list of slots
    public static Slot getSlot(String itemStackName) throws TimeoutException {
        return getAllSlots(itemStackName).get(0);
    }

    // kind of heavy function for what it's doing, but lag-proofing should be very useful and you only need to do this once in while
    public static ItemStack getItemStack(int slotID) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
        return client.player.currentScreenHandler.slots.get(slotID).getStack();
    }

    // returns all slots which have 'itemStackName' in their name
    // tries to get slot whose name contains that name(to account for text color) multiple times(to account for lag)
    // throws TimeoutException if doesn't find needed stack name after set amount of time
    public static List<Slot> getAllSlots(String itemStackName) throws TimeoutException {
        List<Slot> slotList;
        MinecraftClient client = MinecraftClient.getInstance();

        int numberOfTries = 20;
        int i = 0;

        do {
            assert client.player != null;
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

        assert client.player != null;
        return client.player.currentScreenHandler.slots
                .stream().anyMatch(slot ->
                        slot.getStack().getName().getString().contains(itemStackName));
    }

    /**
     * Only checks if stack name contains itemStackName
     * isItemInInventory("Carrot") will return true even if there are only Enchanted Carrots in inventory
     */
    public static boolean isItemInInventory(String itemStackName) {
        MinecraftClient client = MinecraftClient.getInstance();

        assert client.player != null;
        PlayerInventory inventory = client.player.getInventory();

        for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            if (inventory.getStack(i).getName().getString().contains(itemStackName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if stack name equals exactStackName
     * isAmountInInventory("Carrot", 1) will return false if there are only Enchanted Carrots in inventory
     */
    public static boolean isAmountInInventory(String exactStackName, int minCount) {
        MinecraftClient client = MinecraftClient.getInstance();

        assert client.player != null;
        PlayerInventory inventory = client.player.getInventory();

        int count = 0;

        for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.getName().getString().equals(exactStackName)) {
                count += itemStack.getCount();
            }
        }

        return count >= minCount;
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
        try {
            Thread.sleep(2500);
        } catch (InterruptedException ignored) {
        }
    }

    public static void waitForMenu() throws TimeoutException {
        waitForMenuToOpen();
        waitForMenuToLoad();
    }
}
