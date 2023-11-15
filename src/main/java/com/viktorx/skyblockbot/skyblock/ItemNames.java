package com.viktorx.skyblockbot.skyblock;

public enum ItemNames {
    GOD_POT("God Potion"),
    BOOSTER_COOKIE("Booster Cookie"),
    ENCH_CARROT("Enchanted Carrot"),
    ENCH_BROWN_MUSHROOM("Enchanted Brown Mushroom"),
    ENCH_RED_MUSHROOM("Enchanted Red Mushroom"),
    ENCHANTED_SEED("Enchanted Seed"),
    BOX_OF_SEEDS("Box of Seeds"),
    CROPIE("Cropie"),
    CARROT("Carrot"),
    RED_MUSHROOM("Red Mushroom"),
    BROWN_MUSHROOM("Brown Mushroom"),
    MYSTICAL_MUSHROOM_SOUP("Mystical Mushroom Soup"),
    OIL_BARREL("Oil Barrel"),
    ARMADILLO("Armadillo");

    private final String name;

    ItemNames(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
