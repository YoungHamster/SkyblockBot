package com.viktorx.skyblockbot.skyblock;

public enum ItemNames {
    GOD_POT("God Potion"),
    BOOSTER_COOKIE("Booster Cookie"),
    ENCH_CARROT("Enchanted Carrot"),
    ENCH_BROWN_MUSHROOM("Enchanted Brown Mushroom"),
    ENCH_RED_MUSHROOM("Enchanted Red Mushroom"),
    CROPIE("Cropie"),
    CARROT("Carrot"),
    RED_MUSHROOM("Red Mushroom"),
    BROWN_MUSHROOM("Brown Mushroom");

    private final String name;

    ItemNames(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
