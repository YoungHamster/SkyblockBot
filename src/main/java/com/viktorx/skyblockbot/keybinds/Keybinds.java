package com.viktorx.skyblockbot.keybinds;

import com.viktorx.skyblockbot.NotBotCore;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ScoreboardUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Keybinds {
    private static KeyBinding startStopBot;
    private static KeyBinding printTestInfo;

    private static boolean wasPressed = false;

    public static void Init() {
        startStopBot = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.examplemod.spook", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_O, // The keycode of the key
                "category.skyblockbot.toggle" // The translation key of the keybinding's category.
        ));

        printTestInfo = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.examplemod.spook2", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_I, // The keycode of the key
                "category.skyblockbot.getInfo" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (startStopBot.wasPressed()) {
                if (!wasPressed) {
                    NotBotCore.run(client.player);
                } else {
                    NotBotCore.stop(client.player);
                }
                wasPressed = !wasPressed;
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (printTestInfo.wasPressed()) {
                List<String> scores = ScoreboardUtils.getSidebarLines(client.world.getScoreboard());
                scores.forEach(SkyblockBot.LOGGER::info);
            }
        });

    }

    private static void logAllScoreboardInfo(Scoreboard scoreboard) {
        Collection<String> players = scoreboard.getKnownPlayers();
        players.forEach(player -> {
            System.out.println("Player " + player + " objectives[");
            logObjectives(scoreboard, scoreboard.getPlayerObjectives(player));
            System.out.println("]");
        });
        Collection<ScoreboardObjective> objectives = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(i);
            if (objective != null) {
                objectives.add(objective);
            }
        }
        logObjectives(objectives);
    }

    private static void logObjectives(Scoreboard scoreboard, Map<ScoreboardObjective, ScoreboardPlayerScore> objectives) {
        System.out.println("Number of objectives: " + objectives.size());
        objectives.forEach((objective, score) -> {
            System.out.println("Objective{displayName:" + objective.getDisplayName().asString()
                    + ",name:" + objective.getName()
                    + ",criterion-name:" + objective.getCriterion().getName()
                    + ",criterion-toString:" + objective.getCriterion().toString()
                    + ",renderType-name:" + objective.getRenderType().getName()
                    + ",criterion-toString:" + objective.getRenderType().toString()
                    + ",toHoverableText:" + objective.toHoverableText().asString() + "}");
            System.out.println("Score{playerName:" + score.getPlayerName()
                    + ",score:" + score.getScore()
                    + ",toString:" + score.toString() + "}");

            Collection<ScoreboardPlayerScore> scores = scoreboard.getAllPlayerScores(objective);
            System.out.println("Scores by objective:(");
            scores.forEach(score2 -> {
                System.out.println("Score{playerName:" + score2.getPlayerName()
                        + ",score:" + score2.getScore()
                        + ",toString:" + score2.toString() + "}");
            });
            System.out.println(")");
        });
    }

    private static void logObjectives(Collection<ScoreboardObjective> objectives) {
        System.out.println("Number of objectives: " + objectives.size());
        objectives.forEach((objective) -> {
            System.out.println("Objective{displayName:" + objective.getDisplayName().asString()
                    + ",name:" + objective.getName()
                    + ",criterion-name:" + objective.getCriterion().getName()
                    + ",toHoverableText:" + objective.toHoverableText().asString());
        });
    }
}
