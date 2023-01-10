package com.viktorx.skyblockbot.skyblock.flipping;

import com.viktorx.skyblockbot.SkyblockBot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CraftPriceCalculator {
    private static final Map<String, SBRecipe> recipes = new HashMap<>();
    private static boolean wereRecipesLoaded = false;

    public static Float getRecipePrice(String itemName) {
        if (!wereRecipesLoaded) {
            loadRecipes();
            SkyblockBot.LOGGER.info("Loaded recipes");
            wereRecipesLoaded = true;
        }
        SBRecipe recipe = recipes.get(itemName);
        float price = 0.0f;
        for(Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
            Float ingrPrice = PriceFetcher.fetchItemPrice(ingredient.getKey());
            if(ingrPrice != null) {
                price = ingrPrice * ingredient.getValue() + price;
            } else {
                return null; // this is why i can't use map.forEach() here
            }
        }
        return price;
    }

    private static void loadRecipes() {
        String file;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\Nobody\\Documents\\IJProjects\\SkyblockBot\\src\\main\\resources\\recipes.txt"));
            file = reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            SkyblockBot.LOGGER.info("Exception when trying to read recipes.txt");
            e.printStackTrace();
            return;
        }

        int index = file.indexOf(":");
        while (index != -1) {
            String itemName = file.substring(0, index);

            String ingredientsStr = file.substring(file.indexOf("{") + 1, file.indexOf("}"));
            String[] ingredientsArray = ingredientsStr.split("[:,]");
            Map<String, Integer> ingredients = new HashMap<>();
            for (int i = 0; i < ingredientsArray.length / 2; i++) {
                ingredients.put(ingredientsArray[i * 2], Integer.parseInt(ingredientsArray[i * 2 + 1]));
            }
            recipes.put(itemName, new SBRecipe(ingredients, itemName));

            if (file.indexOf("}") == file.length() - 1) {
                file = file.substring(file.indexOf("}") + 1);
            } else {
                file = file.substring(file.indexOf("}") + 2);
            }
            index = file.indexOf(":");
        }
    }

    public static void debugPrintRecipes() {
        if (!wereRecipesLoaded) {
            loadRecipes();
            wereRecipesLoaded = true;
        }
        recipes.forEach((name, recipe) -> SkyblockBot.LOGGER.info(recipe.toString()));
    }

    public static void debugPrintRecipesPrices() {
        if (!wereRecipesLoaded) {
            loadRecipes();
            wereRecipesLoaded = true;
        }
        Map<String, Float> profits = new HashMap<>();
        recipes.forEach((item, recipe) -> {
            Float recipePrice = getRecipePrice(item);
            if(recipePrice != null) {
                Float itemPrice = PriceFetcher.fetchItemPrice(item);
                if(itemPrice != null) {
                    float price = itemPrice - recipePrice - itemPrice*0.01125f;
                    profits.put(item, price); // account for tax
                    SkyblockBot.LOGGER.info(item + ":" + String.format("%.2f", price));
                }
            }
        });
    }
}
