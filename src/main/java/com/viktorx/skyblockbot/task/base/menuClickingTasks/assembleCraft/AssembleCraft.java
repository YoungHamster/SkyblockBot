package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class AssembleCraft extends BaseTask<AssembleCraftExecutor> {
    private CraftableRecipe recipe;

    public AssembleCraft() {
        super(AssembleCraftExecutor.INSTANCE);
    }

    public void setRecipe(CraftableRecipe recipe) {
        this.recipe = recipe;
    }

    public CraftableRecipe getRecipe() {
        return recipe;
    }

    public String getCraftingTableSlotName() {
        return "Crafting Table";
    }
}
