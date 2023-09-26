package com.viktorx.skyblockbot.skyblock.flipping;

import java.util.Map;

public class SBRecipe {
    private Map<String, Integer> ingredients;
    private String result;

    public SBRecipe(Map<String, Integer> ingredients, String result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    public Map<String, Integer> getIngredients() {
        return ingredients;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(result);
        sb.append(":{");

        ingredients.forEach((item, amount) -> sb.append(item).append(":").append(amount).append(","));

        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");

        return sb.toString();
    }
}
