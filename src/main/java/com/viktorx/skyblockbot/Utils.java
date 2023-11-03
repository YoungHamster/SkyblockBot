package com.viktorx.skyblockbot;

import com.viktorx.skyblockbot.mixins.IChatHudMixin;
import com.viktorx.skyblockbot.mixins.IMinecraftClientMixin;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final List<Pair<String, Integer>> prevTickInventory = new ArrayList<>();
    /*
     * Contains full string that was in chat and it's creation tick to prevent any confusions
     */
    private static List<Pair<String, Integer>> detectedStringsInChatIds = new ArrayList<>();

    /**
     * @param str          self-explanatory
     * @param maxBacktrack how many messages it will check in chat, starting from the most recent one
     * @return true if recent messages in chat contain str, otherwise false
     */
    public static boolean isStringInRecentChat(String str, int maxBacktrack) {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        List<ChatHudLine> messages = ((IChatHudMixin) chat).getMessages();
        if (messages.size() == 0) {
            SkyblockBot.LOGGER.warn("BuyItem ERROR! The message history is empty, it's weird");
            return false;
        }

        int limit = Math.min(messages.size(), maxBacktrack);

        /*
         * Clearing out useless data so we don't leak memory when we run for long amounts of time
         */
        if (detectedStringsInChatIds.size() > messages.size()) {
            detectedStringsInChatIds = detectedStringsInChatIds.subList(0, messages.size());
        }

        for (int i = 0; i < limit; i++) {
            Pair<String, Integer> msg = new ImmutablePair<>(messages.get(i).content().getString(), messages.get(i).creationTick());
            if (detectedStringsInChatIds.contains(msg)) {
                continue;
            }
            if (messages.get(i).content().getString().contains(str)) {
                detectedStringsInChatIds.add(msg);
                return true;
            }
        }

        return false;
    }

    public static void InitItemCounter() {
        /*
         * Counting new items in inventory
         */
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            if (client.player.getInventory() == null) {
                return;
            }

            if (prevTickInventory.size() == 0) {
                for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
                    ItemStack stack = client.player.getInventory().getStack(i);
                    prevTickInventory.add(new ImmutablePair<>(stack.getName().getString(), stack.getCount()));
                }
                return;
            }

            for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
                String itemName = client.player.getInventory().getStack(i).getName().getString();
                if (itemName.equals(ItemNames.CARROT.getName()) ||
                        itemName.equals(ItemNames.RED_MUSHROOM.getName()) ||
                        itemName.equals(ItemNames.BROWN_MUSHROOM.getName())) {

                    int prevCount;
                    if (itemName.equals(prevTickInventory.get(i).getLeft())) {
                        prevCount = prevTickInventory.get(i).getRight();
                    } else {
                        prevCount = 0;
                    }

                    int delta = client.player.getInventory().getStack(i).getCount() - prevCount;

                    if (delta > 0) {

                        if (itemName.equals(ItemNames.CARROT.getName())) {
                            GlobalExecutorInfo.carrotCount.addAndGet(delta);
                        } else if (itemName.equals(ItemNames.RED_MUSHROOM.getName())) {
                            GlobalExecutorInfo.redMushroomCount.addAndGet(delta);
                        } else {
                            GlobalExecutorInfo.brownMushroomCount.addAndGet(delta);
                        }
                    }
                }
            }

            prevTickInventory.clear();
            for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                prevTickInventory.add(new ImmutablePair<>(stack.getName().getString(), stack.getCount()));
            }
        });
    }

    public static void sendChatMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            SkyblockBot.LOGGER.error("Can't send chat message! Current screen isn't null, so i can't open chat screen");
            return;
        }

        ((IMinecraftClientMixin) client).callOpenChatScreen(message);
        client.currentScreen.keyPressed(InputUtil.GLFW_KEY_ENTER, 0, 0);
    }

    public static File getLastModified(String directoryFilePath) {
        File directory = new File(directoryFilePath);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        return chosenFile;
    }

    private static int value(char r) {
        if (r == 'I')
            return 1;
        if (r == 'V')
            return 5;
        if (r == 'X')
            return 10;
        if (r == 'L')
            return 50;
        if (r == 'C')
            return 100;
        if (r == 'D')
            return 500;
        if (r == 'M')
            return 1000;
        return -1;
    }

    //function to convert roman to integer
    public static int convertRomanToInt(String s) {
        //variable to store the sum
        int total = 0;
        //loop iterate over the string (given roman numeral)
        //getting value from symbol s1[i]
        for (int i = 0; i < s.length(); i++) {
            int s1 = value(s.charAt(i));
            //getting value of symbol s2[i+1]

            if (i + 1 < s.length()) {
                int s2 = value(s.charAt(i + 1));
                //comparing the current character from its right character
                if (s1 >= s2) {
                    //if the value of current character is greater or equal to the next symbol
                    total = total + s1;
                } else {
                    //if the value of the current character is less than the next symbol
                    total = total - s1;
                }
            } else {
                total = total + s1;
            }
        }
        //returns corresponding integer value
        return total;
    }

    public static String getSBApiPage(String URL) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET() // GET is default
                .build();

        String result = "";

        do {
            try {
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                result = response.body();
            } catch (IOException | InterruptedException bs) {
                SkyblockBot.LOGGER.info("Some bs happened, no connection to hypixel api");
            }
        } while (!result.startsWith("{\"success\":true"));

        return result;
    }

    // basically if you have "{"stuff":"stuff","otherstuff":420}"
    // and give it "\"stuff\"" as an argument it will return
    // ",\"otherstuff\":420}" and "\"stuff\""
    // if you give it "\"otherstuff\"" it will return } and "420"
    public static Pair<String, String> getJSONTokenAndCutItOut(String token, String json) {
        // is this shitcode or was there really no better way of doing it?
        // who knows
        int index = json.indexOf(token);
        if (index == -1) {
            SkyblockBot.LOGGER.info("Wrong args for getJSONTokenAndCutItOut function in Utils\n" +
                    "token: " + token + "\njson: " + json);
            return new ImmutablePair<>(null, null);
        }

        json = json.substring(index);
        int tokenValueStart = json.indexOf(":") + 1;

        try {
            if (json.indexOf("}") < json.indexOf(",") || !json.contains(",")) {
                return new ImmutablePair<>(json.substring(json.indexOf("}") + 1),
                        json.substring(tokenValueStart, json.indexOf("}"))
                );
            } else {
                return new ImmutablePair<>(json.substring(json.indexOf(",") + 1),
                        json.substring(tokenValueStart, json.indexOf(","))
                );
            }
        } catch (Exception e) {
            SkyblockBot.LOGGER.info(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static float normalize(float value, float start, float end) {
        float width = end - start;   //
        float offsetValue = value - start;   // value relative to 0

        return (offsetValue - ((float) Math.floor(offsetValue / width) * width)) + start;
        // + start to reset back to start of original range
    }

    // default range from -180 to 180
    public static float normalize(float value) {
        return normalize(value, -180.0f, 180.0f);
    }

    public static double distanceBetween(Vec3d v1, Vec3d v2) {
        return v1.add(v2.multiply(-1.0d)).length();
    }
}
