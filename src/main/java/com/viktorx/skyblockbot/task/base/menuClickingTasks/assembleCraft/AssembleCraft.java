package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.skyblock.flipping.SBRecipe;
import com.viktorx.skyblockbot.task.Task;

public class AssembleCraft extends Task {
    private SBRecipe recipe;

    public void setRecipe(SBRecipe recipe) {
        this.recipe = recipe;
    }

    public SBRecipe getRecipe() {
        return recipe;
    }

    public String getCraftingTableSlotName() {
        return "";
    }
}
