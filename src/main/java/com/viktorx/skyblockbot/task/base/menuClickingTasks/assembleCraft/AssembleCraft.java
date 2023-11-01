package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.skyblock.flipping.SBRecipe;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor.TalkToVisitorExecutor;

public class AssembleCraft extends Task {
    private SBRecipe recipe;


    public void execute() {
        AssembleCraftExecutor.INSTANCE.execute(this);
    }
    public void pause() {
        AssembleCraftExecutor.INSTANCE.pause();
    }
    public void resume() {
        AssembleCraftExecutor.INSTANCE.resume();
    }
    public void abort() {
        AssembleCraftExecutor.INSTANCE.abort();
    }
    public boolean isExecuting() {
        return AssembleCraftExecutor.INSTANCE.isExecuting(this);
    }
    public boolean isPaused() {
        return AssembleCraftExecutor.INSTANCE.isPaused();
    }

    public void setRecipe(SBRecipe recipe) {
        this.recipe = recipe;
    }

    public SBRecipe getRecipe() {
        return recipe;
    }

    public String getCraftingTableSlotName() {
        return "Crafting Table";
    }
}
