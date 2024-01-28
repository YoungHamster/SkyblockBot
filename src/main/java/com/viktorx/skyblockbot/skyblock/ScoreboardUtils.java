package com.viktorx.skyblockbot.skyblock;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.viktorx.skyblockbot.SkyblockBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Texts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardUtils {

    /**
     * Fetching lines are based on how they're visually seen on your sidebar
     * and not based on the actual value score.
     * <p>
     * Written around Minecraft 1.8 Scoreboards, modify to work with your
     * current version of Minecraft.
     * <p>
     * <3 aaron1998ish
     * ахуеть спасибо папаша аарон1998 что подарил мне код который парсит эту залупу чёрного цвета двадцатого века
     * 2 дня подряд блять по дню ебусь над мелкой хуйнёй
     *
     * @return a list of lines for a given scoreboard or empty
     * if the worlds not loaded, scoreboard isnt present
     * or if the sidebar objective isnt created.
     */
    public static List<String> getLines(ScoreboardDisplaySlot slot) {
        List<String> lines = new ArrayList<>();
        assert MinecraftClient.getInstance().world != null;
        Scoreboard scoreboard = MinecraftClient.getInstance().world.getScoreboard();
        if (scoreboard == null) {
            return lines;
        }

        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(slot);

        if (objective == null) {
            return lines;
        }

        Collection<ScoreHolder> scoreHolders = scoreboard.getKnownScoreHolders();

        List<ScoreHolder> list = scoreHolders.stream().filter(input -> input != null &&
                input.getNameForScoreboard() != null &&
                !input.getNameForScoreboard().startsWith("#") &&
                scoreboard.getScore(input, objective) != null
        ).collect(Collectors.toList());

        if (list.size() > 15) {
            scoreHolders = Lists.newArrayList(Iterables.skip(list, scoreHolders.size() - 15));
        } else {
            scoreHolders = list;
        }

        scoreHolders = scoreHolders
                        .stream()
                        .sorted(Comparator.comparingInt(scrHldr -> scoreboard.getScore(scrHldr, objective).getScore()))
                        .collect(Collectors.toList());

        for (ScoreHolder scoreHolder : scoreHolders) {
            Team team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());
            String line = Team.decorateName(team, Texts.toText(new MessageImpl(scoreHolder.getNameForScoreboard()))).getString();
            line = line.replaceAll("§.", "");

            lines.add(line);
        }

        return lines;
    }
}
