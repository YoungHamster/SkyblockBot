package com.viktorx.skyblockbot.skyblock.flipping;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class SBRecipe {
    Map<String, Integer> ingridients;
    String result;

    public SBRecipe(Map<String, Integer> ingridients, String result) {
        this.ingridients = ingridients;
        this.result = result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(result);
        sb.append(":{");
        ingridients.forEach((item, amount) -> sb.append(item).append(":").append(amount).append(","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
