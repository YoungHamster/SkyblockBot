package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.SkyblockBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SBUtils {
    // gets profile balance from tab, sums personal bank balance and coop bank
    public static long getBankBalance() {
        String bankLine = getTabPlayers().stream().filter(string -> string.contains("Bank")).collect(Collectors.joining());
        if(bankLine.length() == 0) {
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
        if(purseLine.length() == 0) {
            return -1;
        }
        return (long) Float.parseFloat(purseLine.replace("Purse:", ""));
    }

    public static String getSlotText(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player.getInventory().getStack(slot).getName().getString();
    }

    // tries to get slot with that name multiple times to account for lag
    // throws TimeoutException if doesn't find needed stack name after set amount of time
    public static Slot getSlot(String itemStackName) throws TimeoutException {
        List<Slot> slotList;
        MinecraftClient client = MinecraftClient.getInstance();
        int numberOfTries = 25;
        int i = 0;
        do {
            slotList = client.player.currentScreenHandler.slots.stream()
                    .filter(slot -> slot.getStack().getName().getString().contains(itemStackName))
                    .collect(Collectors.toList());

            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {}
            i++;
            if(i > numberOfTries) {
               throw new TimeoutException();
            }
        } while (slotList.size() == 0);
        return slotList.get(0);
    }
}
