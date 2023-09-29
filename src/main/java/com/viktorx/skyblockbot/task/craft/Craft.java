package com.viktorx.skyblockbot.task.craft;

import com.viktorx.skyblockbot.task.Task;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

public class Craft implements Task {
    private ItemStack[] craftingTableSlots = new ItemStack[9];

    Craft(Map<String, Integer> recipe) {
        for(int i = 0; i < 9; i++) {
            craftingTableSlots[i] = recipe.
        }
    }

    @Override
    public void execute() {

    }

    @Override
    public void saveToFile(String filename) {

    }
}
