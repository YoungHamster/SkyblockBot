package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.flipping.SBRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class SBProfile {
    private final List<SBSkill> skills = new ArrayList<>();
    private final List<SBRecipe> unlockedRecipes = new ArrayList<>();
    private long bankBalance;
    private long purse;

    public void loadData() throws TimeoutException {
        purse = SBUtils.getPurse();
        bankBalance = SBUtils.getBankBalance();
        loadSkills();
    }

    private void loadSkills() throws TimeoutException {
        MinecraftClient client = MinecraftClient.getInstance();
        Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[8]); // selects hotbar slot 9(sb menu)
        Keybinds.asyncPressKeyAfterTick(client.options.useKey); // opens sb menu with rmb click
        SBUtils.waitForMenu();
        SBUtils.leftClickOnSlot("Your Skills");
        SBUtils.waitForMenu();

        skills.add(new SBSkill(SBUtils.getSlot("Farming").getStack().getName().getString()));
        skills.add(new SBSkill(SBUtils.getSlot("Mining").getStack().getName().getString()));
        skills.add(new SBSkill(SBUtils.getSlot("Combat").getStack().getName().getString()));
        skills.add(new SBSkill(SBUtils.getSlot("Foraging").getStack().getName().getString()));
        skills.add(new SBSkill(SBUtils.getSlot("Fishing").getStack().getName().getString()));
        skills.add(new SBSkill(SBUtils.getSlot("Enchanting").getStack().getName().getString()));
        skills.add(new SBSkill(SBUtils.getSlot("Alchemy").getStack().getName().getString()));

        Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
    }

    public void loadUnlockedRecipes() throws TimeoutException {
        MinecraftClient client = MinecraftClient.getInstance();
        Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[8]); // selects hotbar slot 9(sb menu)
        Keybinds.asyncPressKeyAfterTick(client.options.useKey); // opens sb menu with rmb click
        SBUtils.waitForMenu();

        SBUtils.leftClickOnSlot("Recipe Book");
        SBUtils.waitForMenu();
        List<Slot> recipeSlots = new ArrayList<>();
        try {
            recipeSlots.add(SBUtils.getSlot("Farming"));
            recipeSlots.add(SBUtils.getSlot("Mining"));
            recipeSlots.add(SBUtils.getSlot("Combat"));
            recipeSlots.add(SBUtils.getSlot("Fishing"));
            recipeSlots.add(SBUtils.getSlot("Foraging"));
            recipeSlots.add(SBUtils.getSlot("Special"));
        } catch (TimeoutException ignored) {}
        for (Slot slot : recipeSlots) {
            SBUtils.leftClickOnSlot(slot);
            SBUtils.waitForMenu();
            // check if there is 'next page' button
            while (SBUtils.anySlotsWithName("Next Page")) {
                unlockedRecipes.addAll(parseRecipesMenuPage());
                saveshit();
                SBUtils.leftClickOnSlot("Next Page");
                SBUtils.waitForMenu();
            }
            unlockedRecipes.addAll(parseRecipesMenuPage());
            saveshit();
            SBUtils.leftClickOnSlot("Go Back");
            SBUtils.waitForMenu();
        }
        SkyblockBot.LOGGER.info("Closing inventory");
        Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
    }

    // like those pages, when you press 'farming recipes' and there is first page, then second, etc
    // assumes page is already open
    private List<SBRecipe> parseRecipesMenuPage() throws TimeoutException {
        List<SBRecipe> recipes = new ArrayList<>();
        // some magic numbers to basically click on every recipe in the menu which are located you know how
        for (int i = 1; i < 5; i++) {
            for (int j = 1; j < 8; j++) {
                int slotID = j + i * 9;
                // skipping empty slots, locked slots, minions and pets
                if (SBUtils.getSlotText(slotID).contains("???")
                        || SBUtils.getSlotText(slotID).contains("Minion")
                        || SBUtils.getSlotText(slotID).contains("Air")
                        || SBUtils.getSlotText(slotID).contains(" Pet")) {
                    continue;
                }
                SBUtils.leftClickOnSlot(slotID);
                SBUtils.waitForMenu();
                recipes.add(parseSBRecipe());
                SBUtils.leftClickOnSlot("Go Back");
                SBUtils.waitForMenu();
            }
        }
        return recipes;
    }

    private SBRecipe parseSBRecipe() throws TimeoutException {
        String result = SBUtils.getSlotText(25);
        SkyblockBot.LOGGER.info("Parsing recipe for: " + result);
        Map<String, Integer> ingredients = new HashMap<>();
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                int slotID = j + i * 9;
                ItemStack item = SBUtils.getItemStack(slotID);
                String name = item.getName().getString();
                if (name.contains("Air")) {
                    continue;
                }
                int count = item.getCount();
                if (ingredients.containsKey(name)) {
                    ingredients.put(name, ingredients.get(name) + count);
                } else {
                    ingredients.put(name, count);
                }
            }
        }
        return new SBRecipe(ingredients, result);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bank balance: ").append(bankBalance).append("\n");
        sb.append("Purse: ").append(purse).append("\n");
        sb.append("Skills: {");
        skills.forEach(sbSkill -> sb.append(sbSkill.toString()).append(","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}").append("\n");
        sb.append("Unlocked recipes: {");
        unlockedRecipes.forEach(sbRecipe -> sb.append(sbRecipe.toString()).append(","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}").append("\n");
        return sb.toString();
    }

    private void saveshit() {
        String sb = toString();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Nobody\\Desktop\\SBBotTestInfo.txt"));
            writer.write(sb);
            writer.close();
        } catch (IOException ignored) {

        }
    }
}
