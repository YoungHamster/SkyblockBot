package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import javafx.util.Pair;

import java.util.List;

public class CraftableRecipe {
    private final List<Pair<String, Integer>> ingredients;

    public CraftableRecipe(List<Pair<String, Integer>> ingredients) {
        this.ingredients = ingredients;
    }

    public Pair<String, Integer> getIngredient(int i) {
        return ingredients.get(i);
    }

    public List<Pair<String, Integer>> getIngredients() {
        return ingredients;
    }
}
