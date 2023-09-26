package com.viktorx.skyblockbot.skyblock.flipping;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.flipping.flips.CraftFlip;
import com.viktorx.skyblockbot.skyblock.flipping.flips.FlipFactory;
import com.viktorx.skyblockbot.skyblock.flipping.flips.PotentialFlip;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CraftPriceCalculator {
    private static CraftPriceCalculator instance;

    private final Map<String, SBRecipe> recipes = new HashMap<>();

    private CraftPriceCalculator() {
        loadRecipes();
        SkyblockBot.LOGGER.info("Loaded recipes");
    }

    public static synchronized CraftPriceCalculator getInstance() {
        if (instance == null) {
            instance = new CraftPriceCalculator();
        }
        return instance;
    }

    public Double getRecipePrice(String itemName) {
        SBRecipe recipe = recipes.get(itemName);
        double price = 0.0f;

        for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
            Double ingrPrice = PriceDatabase.getInstance().fetchItemPrice(ingredient.getKey());
            if (ingrPrice != null) {
                price = ingrPrice * ingredient.getValue() + price;
            } else {
                return null; // this is why i can't use map.forEach() here
            }
        }
        return price;
    }

    private void loadRecipes() {
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

    public void debugPrintRecipesPrices() {
        List<PotentialFlip> flips = new ArrayList<>();

        recipes.forEach((item, recipe) -> {
            PotentialFlip pf = FlipFactory.createFlip(CraftFlip.type, item);
            if (pf != null && pf.getOneFlipInvestment() > 10000.0d) {
                flips.add(pf);
            }
        });

        flips.sort(PotentialFlip::comparingBy24hProfit);
        flips.forEach(flip -> SkyblockBot.LOGGER.info(
                        String.format("%s:{ 24h profit: %.1f, 24h investment: %.1f, one flip profit: %.1f, one flip investment: %.1f",
                                flip.getItemName(),
                                flip.getPotential24hProfit(),
                                flip.get24hInvestment(),
                                flip.getOneFlipProfit(),
                                flip.getOneFlipInvestment()
                        )
                )
        );
    }
}
