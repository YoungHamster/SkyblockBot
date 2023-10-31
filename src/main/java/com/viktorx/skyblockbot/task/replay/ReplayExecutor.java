package com.viktorx.skyblockbot.task.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.replay.tickState.AnyKeyRecord;
import com.viktorx.skyblockbot.task.replay.tickState.TickState;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class ReplayExecutor {

    public static final ReplayExecutor INSTANCE = new ReplayExecutor();
    private final List<String> itemsWhenStarted = new ArrayList<>();
    private final List<TickState> lastNTicks = new ArrayList<>();
    private final List<String> whitelistedBlocks = new ArrayList<>();
    private final Queue<AnyKeyRecord> newKeys = new ArrayBlockingQueue<>(15);
    private final Map<Integer, AnyKeyRecord> currentlyPressedKeys = new HashMap<>();
    public boolean serverChangedPositionRotation = false;
    public boolean serverChangedItem = false;
    public boolean serverChangedSlot = false;
    public int debugPacketCounter = 0;
    public int debugOnGroundOnlyCounter = 0;
    public int debugLookAndOnGroundCounter = 0;
    public int debugPositionAndOnGroundCounter = 0;
    public int debugFullCounter = 0;
    private Replay replay;
    private int tickIterator;
    private ReplayBotState state = ReplayBotState.IDLE;
    private CompletableFuture<Void> yawTask = null;
    private CompletableFuture<Void> pitchTask = null;
    private int antiDetectTriggeredTickCounter = 0;

    public void Init() {
        whitelistedBlocks.add("oak_sign");
        whitelistedBlocks.add("iron_door");

        ClientTickEvents.START_CLIENT_TICK.register(this::onTickRec);
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickPlay);
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickAntiDetectTriggered);
    }

    private void onTickRec(MinecraftClient client) {
        if (!state.equals(ReplayBotState.RECORDING)) {
            return;
        }

        assert client.player != null;
        if (!detectAndCorrectLagBack(client.player)) {
            return;
        }

        replay.addTickState(getCurrentTickState());
    }

    public void startRecording() {
        if (!state.equals(ReplayBotState.IDLE)) {
            SkyblockBot.LOGGER.info("Can't start recording when state = " + state.getName());
            return;
        }

        state = ReplayBotState.NOT_IDLE;

        SkyblockBot.LOGGER.info("started recording");
        replay = new Replay();

        debugPacketCounter = 0;
        debugOnGroundOnlyCounter = 0;
        debugLookAndOnGroundCounter = 0;
        debugPositionAndOnGroundCounter = 0;
        debugFullCounter = 0;

        state = ReplayBotState.RECORDING;
    }

    public void stopRecording() {
        SkyblockBot.LOGGER.info("stopped recording");

        printDebugInfo();
        saveRecordingAsync();

        state = ReplayBotState.IDLE;
    }

    private void antiDetectDone() {
        TGBotDaemon.INSTANCE.takeAndSendScreenshot("Anti detect triggered!!!", true);

        unpressButtons();
        if (ReplayBotSettings.autoQuitWhenAntiDetect) {
            MinecraftClient.getInstance().stop();
        }
    }

    private void onTickAntiDetectTriggered(MinecraftClient client) {
        if (!state.equals(ReplayBotState.ANTI_DETECT_TRIGGERED)) {
            return;
        }

        if (antiDetectTriggeredTickCounter++ == ReplayBotSettings.antiDetectTriggeredWaitTicks || tickIterator == 0) {
            abort();
            antiDetectDone();
            return;
        }

        asyncPlayAlarmSound();
        this.pressButtons(replay.getTickState(tickIterator));

        Vec2f prevRot = replay.getTickState(tickIterator - 1).getRotation();
        Vec2f currentRot = replay.getTickState(tickIterator).getRotation();
        Vec2f deltaCam = currentRot.add(prevRot.multiply(-1));

        assert client.player != null;
        client.player.setYaw(client.player.getYaw() + deltaCam.x);
        client.player.setPitch(client.player.getPitch() + deltaCam.y);

        tickIterator++;
        if (tickIterator == replay.size()) {
            abort();
            antiDetectDone();
        }
    }

    private void onTickPlay(MinecraftClient client) {
        if (!state.equals(ReplayBotState.PLAYING)) {
            if (state.equals(ReplayBotState.PREPARING_TO_START)) {
                if (!yawTask.isDone() || !pitchTask.isDone()) {
                    return;
                } else {
                    state = ReplayBotState.UNPRESSING_BUTTONS_BEFORE_START;
                }
            } else if (state.equals(ReplayBotState.UNPRESSING_BUTTONS_BEFORE_START)) {
                state = ReplayBotState.PLAYING;
                return;
            } else {
                return;
            }
        }

        if (GlobalExecutorInfo.worldLoading.get()) {
            abort();
            return;
        }

        if (antiDetect(client)) {
            state = ReplayBotState.NOT_IDLE;
            asyncPlayAlarmSound();
            state = ReplayBotState.ANTI_DETECT_TRIGGERED;
            TGBotDaemon.INSTANCE.takeAndSendScreenshot("Anti detect triggered!!!", true);
            return;
        }

        TickState tickState = replay.getTickState(tickIterator);

        assert client.player != null;

        tickState.setRotationForClient(client);
        tickState.setPositionForClient(client);
        this.pressButtons(tickState);

        tickIterator++;
        if (tickIterator == replay.size()) {
            tickIterator = 0;

            printDebugInfo();
            unpressButtons();
            state = ReplayBotState.IDLE;
            replay.completed();
        }
    }


    public synchronized void execute(Replay replay) throws InterruptedException {
        if (!state.equals(ReplayBotState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't play while state = " + state.getName());
            return;
        }

        this.replay = replay;

        if (replay.size() == 0) {
            SkyblockBot.LOGGER.warn("can't start playing, nothing to play");
            state = ReplayBotState.IDLE;
            abort();
            return;
        }

        state = ReplayBotState.NOT_IDLE;
        tickIterator = 0;

        assert MinecraftClient.getInstance().player != null;

        /*
         * If tick 0 from replay isn't close to player's current position we attempt to find replay tick close enough
         * to current position
         * If we do we can just start for the middle of the replay instead of refusing to work
         */
        if (!isPlayerInCorrectPosition()) {
            if (tryStartingFromMiddleOfRecording(replay)) return;
        }

        SkyblockBot.LOGGER.info("Starting playing");

        itemsWhenStarted.clear();
        for (int i = 0; i < 9; i++) {
            itemsWhenStarted.add(MinecraftClient.getInstance().player.getInventory().getStack(i).getItem().getName().getString());
        }

        debugPacketCounter = 0;
        debugOnGroundOnlyCounter = 0;
        debugLookAndOnGroundCounter = 0;
        debugPositionAndOnGroundCounter = 0;
        debugFullCounter = 0;

        antiDetectTriggeredTickCounter = 0;

        prepareToStart();
    }

    private boolean tryStartingFromMiddleOfRecording(Replay replay) {
        SkyblockBot.LOGGER.info("Trying to find fitting tick state away from the start");

        double lowestDistance = ReplayBotSettings.maxDistanceToFirstPoint;
        int lowestDistanceIterator = -1;
        for (int i = 0; i < replay.tickStates.size(); i++) {
            tickIterator = i;
            double distanceToStartPoint = getDistanceToExpectedPosition();

            if (distanceToStartPoint < lowestDistance) {
                lowestDistance = distanceToStartPoint;
                lowestDistanceIterator = i;
            }
        }

        if (lowestDistance < ReplayBotSettings.maxDistanceToFirstPoint) {

            tickIterator = lowestDistanceIterator;

            /*
             * Figure out which keys are pressed at the moment we start, press them before starting
             */
            Map<Integer, AnyKeyRecord> keys = findKeysPressedAtTick(tickIterator);
            keys.forEach((key, value) -> {
                value.firstPress();
                currentlyPressedKeys.put(key, value);
            });

            SkyblockBot.LOGGER.info("Found tick to start from! Continuing replay from " + lowestDistanceIterator + " tick state");
        } else {
            SkyblockBot.LOGGER.warn(
                    "Can't find fitting point. Distance to first point: " + getDistanceToExpectedPosition());
            state = ReplayBotState.IDLE;
            abort();
            return true;
        }
        return false;
    }

    public synchronized void abort() {
        if (state.equals(ReplayBotState.IDLE)) {
            SkyblockBot.LOGGER.info("Can't abort replay, already idle");
            return;
        }
        unpressButtons();
        tickIterator = 0;

        SkyblockBot.LOGGER.info("aborted playing");
        state = ReplayBotState.IDLE;

        printDebugInfo();

        replay.aborted();
    }

    /*
     * IMPORTANT: call this method before doing anything to player like changing rotation or position
     * anti-detection stuff
     * check for correct rotation
     * check what item is in hand(anti-captcha)
     */
    private boolean antiDetect(@NotNull MinecraftClient client) {
        if (!isNextStatePossible(client)) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: next position is impossible, must be wall check, stopping the bot");
            return true;
        }

        if (serverChangedItem) {
            serverChangedItem = false;

            assert client.player != null;
            String expected = itemsWhenStarted.get(client.player.getInventory().selectedSlot);
            String current = client.player.getInventory().getMainHandStack().getItem().getName().getString();
            if (!current.equals(expected)) {
                SkyblockBot.LOGGER.warn("Anti-detection alg: server changed item in hand. Expected: " + expected
                        + ", current: " + current);
                return true;
            }
        }

        if (serverChangedPositionRotation) {
            serverChangedPositionRotation = false;

            if (tickIterator > 0) {
                assert client.player != null;
                if (!checkPosAdjustLag(client.player)) {
                    SkyblockBot.LOGGER.warn("Anti-detection alg: server changed position, can't adjust for it, stopping the bot");
                    return true;
                }

                if (!checkRot(client.player)) {
                    SkyblockBot.LOGGER.warn("Anti-detection alg: server changed rotation, can't adjust for it");
                    return true;
                }
            }
        }

        if (serverChangedSlot) {
            serverChangedSlot = false;

            assert client.player != null;
            String expected = itemsWhenStarted.get(client.player.getInventory().selectedSlot);
            String current = client.player.getInventory().getMainHandStack().getItem().getName().getString();
            if (!current.equals(expected)) {
                SkyblockBot.LOGGER.warn("Anti-detection alg: server changed slot. Expected item in hand: " + expected
                        + ", current: " + current);
                return true;
            }
        }

        return false;
    }

    private boolean isNextStatePossible(@NotNull MinecraftClient client) {
        assert client.player != null;
        for (int i = 0; i < ReplayBotSettings.checkForCollisionsAdvanceTicks && tickIterator + i < replay.size(); i++) {

            /*
             * I want to round the Y correctly
             */
            Vec3d pos = replay.getTickState(tickIterator + i).getPosition();
            pos = new Vec3d(Math.floor(pos.x), Math.rint(pos.y), Math.floor(pos.z));

            BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
            BlockPos above = new BlockPos(blockPos).up();

            if (client.world == null) {
                SkyblockBot.LOGGER.warn("client.world == null wtf????????");
                return true;
            }

            boolean isBlockSolid = client.world.getBlockState(blockPos).blocksMovement();
            boolean isBlockAboveSolid = client.world.getBlockState(above).blocksMovement();

            if (isBlockSolid || isBlockAboveSolid) {
                String blockName = client.world.getBlockState(blockPos).getBlock().asItem().toString();
                String blockAboveName = client.world.getBlockState(above).getBlock().asItem().toString();

                for (String name : whitelistedBlocks) {
                    if (blockName.equals(name)) {
                        isBlockSolid = false;
                    }
                    if (blockAboveName.equals(name)) {
                        isBlockAboveSolid = false;
                    }
                }

                // Check again after accounting for whitelisted blocks
                if (isBlockSolid || isBlockAboveSolid) {
                    SkyblockBot.LOGGER.info(
                            "Block at " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ()
                                    + " : " + blockName + " solid-" + isBlockSolid
                                    + "\nBlock above: " + blockAboveName + " solid-" + isBlockAboveSolid);

                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Corrects position
     * return - true if it is correct or was corrected (correct by changing tickIterator to index of state closest to current state)
     *          false if the position can't be corrected(which means player was teleported to check for macros or lagged back too far)
     */
    private boolean checkPosAdjustLag(@NotNull ClientPlayerEntity player) {
        TickState state = replay.getTickState(tickIterator - 1);
        double deltaExpectedPos = Utils.distanceBetween(player.getPos(), state.getPosition());
        if (deltaExpectedPos > ReplayBotSettings.reactToLagbackThreshold) {
            // if delta is too big something is wrong, check if we were simply lagged back and if not-stop
            double minDelta = ReplayBotSettings.reactToLagbackThreshold;
            int bestTickIndex = -1;

            for (int i = 1; i <= ReplayBotSettings.maxLagbackTicks && tickIterator >= i; i++) {
                double delta = Utils.distanceBetween(player.getPos(), replay.getTickState(tickIterator - i).getPosition());

                /*
                 * If delta is < max then we're roughly at the correct tick
                 * Press correct buttons, set correct rotation
                 * If delta is < min then we don't have to correct position, otherwise correct it
                 */
                if (delta < minDelta) {
                    minDelta = delta;
                    bestTickIndex = tickIterator - i;
                }
            }
            if (bestTickIndex != -1) {
                if (isStuck(player)) {
                    // TODO do something
                    SkyblockBot.LOGGER.info("Detected being stuck. Shutting down for now, will do something else later");
                    return false;
                }
                tickIterator = bestTickIndex;


                /*
                 * Figure out which keys are pressed at the tick we lagged back to, press them before continuing
                 * Also unpress keys that weren't pressed at that tick
                 */
                Map<Integer, AnyKeyRecord> keys = findKeysPressedAtTick(tickIterator);

                keys.forEach((key, value) -> {
                    if(!currentlyPressedKeys.containsKey(key)) {
                        currentlyPressedKeys.put(key, value);
                        value.firstPress();
                    }
                });

                List<Integer> keysToRemove = new ArrayList<>();
                currentlyPressedKeys.forEach((key, value) -> {
                    if(!keys.containsKey(key)) {
                        value.unpress();
                        keysToRemove.add(key);
                    }
                });
                keysToRemove.forEach(currentlyPressedKeys::remove);

                SkyblockBot.LOGGER.info("Adjusted for lagback. Min delta = " + minDelta);
                return true;
            }
            /* If previous states aren't close to current position it must not be lagback, but teleport or something else */
            return false;
        }
        return true;
    }

    public boolean isPlayerInCorrectPosition() {
        return getDistanceToExpectedPosition() < ReplayBotSettings.maxDistanceToFirstPoint;
    }

    private double getDistanceToExpectedPosition() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Vec3d expected = replay.getTickState(tickIterator).getPosition();
        assert player != null;
        Vec3d actual = player.getPos();

        return actual.subtract(expected).length();
    }

    /*
     * This thing at the moment is only called when server lags back the player(not every tick)
     * But my guess right now is that it's still gonna do what I intend it to do,
     *  because "stuckness coefficient" will be higher when that method is called rarely, not lower
     */
    private boolean isStuck(@NotNull ClientPlayerEntity player) {
        lastNTicks.add(getCurrentTickState());
        if (lastNTicks.size() > ReplayBotSettings.antiStucknessTickCount) {
            lastNTicks.remove(0);
        } else {
            // don't actually check for stuckness when we're just starting to move
            return false;
        }

        double movedLastNTicks = Utils.distanceBetween(
                lastNTicks.get(0).getPosition(),
                lastNTicks.get(lastNTicks.size() - 1).getPosition()
        );

        double expectedToMove = Utils.distanceBetween(
                player.getPos(),
                replay.getTickState(tickIterator - lastNTicks.size()
                ).getPosition()
        );

        if (movedLastNTicks / expectedToMove <= ReplayBotSettings.detectStucknessCoefficient) {
            SkyblockBot.LOGGER.warn("Bot is stuck! moved in last ticks: " + movedLastNTicks + ", expected to move: " + expectedToMove);
            return true;
        }
        return false;
    }

    private boolean checkRot(@NotNull ClientPlayerEntity player) {
        float dPitch = Math.abs(replay.getTickState(tickIterator - 1).getPitch() - player.getPitch());
        float expectedYaw = Utils.normalize(replay.getTickState(tickIterator - 1).getYaw());
        float currentYaw = Utils.normalize(player.getYaw());

        float dYaw = Math.abs(expectedYaw - currentYaw);
        float dYaw2 = Math.abs(Math.abs(expectedYaw - currentYaw) - 360);

        float threshold = ReplayBotSettings.antiDetectDeltaAngleThreshold;

        if (!(dPitch < threshold && (dYaw < threshold || dYaw2 < threshold))) {
            SkyblockBot.LOGGER.info("Can't fix rotation. dYaw = " + dYaw + ", dPitch = " + dPitch + ", current yaw = " + currentYaw + ", expected yaw = " + expectedYaw);
            return false;
        } else {
            return true;
        }
    }

    /* Used for recording only */
    private boolean detectAndCorrectLagBack(@NotNull ClientPlayerEntity player) {
        if (serverChangedPositionRotation) {
            serverChangedPositionRotation = false;

            int bestFit = -1;
            double bestFitDistance = ReplayBotSettings.reactToLagbackThreshold;

            for (int i = 0; i < ReplayBotSettings.maxLagbackTicksWhenRecording && replay.size() > i; i++) {
                int index = replay.size() - i - 1;

                double distance = Utils.distanceBetween(
                        player.getPos(),
                        replay.getTickState(index).getPosition()
                );

                if (distance < bestFitDistance) {
                    bestFit = index;
                    bestFitDistance = distance;
                }
            }

            if (bestFit != -1) {
                replay.deleteFromIndexToIndex(bestFit + 1, replay.size());
                SkyblockBot.LOGGER.info("Lagged back when recording, corrected! Best fit distance = " + bestFitDistance);
            } else {
                SkyblockBot.LOGGER.warn("Lagged back when recording, can't correct, stopping the recording");
                stopRecording();
                return false;
            }
        }
        return true;
    }

    private TickState getCurrentTickState() {
        List<AnyKeyRecord> keys = new ArrayList<>();
        while (!newKeys.isEmpty()) {
            keys.add(newKeys.poll());
        }
        return new TickState(keys);
    }

    public void onKeyPress(AnyKeyRecord newKey) {
        if (state.equals(ReplayBotState.RECORDING)) {
            newKeys.add(newKey);
        }
    }

    /*
     * I could keep track of all presses and unpresses but it would be a lot of work, steal performance and be a pain altogether
     * Instead i just unpress every button that i could want to be unpressed
     */
    private void unpressButtons() {
        currentlyPressedKeys.forEach((key, value) -> {
            value.unpress();
        });
        currentlyPressedKeys.clear();
    }

    private void pressButtons(TickState tickState) {
        List<AnyKeyRecord> tickKeys = tickState.getKeys();

        for (AnyKeyRecord key : tickKeys) {
            /*
             * We don't actually need to account for situation where key is already pressed, but not in the map of pressed keys
             * because we ensure continuity of key presses from start to finish of execution
             */
            switch (key.getAction()) {
                case 0 -> {
                    currentlyPressedKeys.remove(key.getKey());
                    key.press();
                }

                case 1 -> {
                    currentlyPressedKeys.put(key.getKey(), key);
                    key.press();
                }

                case 2 -> key.press();

                default -> SkyblockBot.LOGGER.warn("Key action is not 0, 1 or 2, its: " + key.getAction());
            }
        }
    }

    private Map<Integer, AnyKeyRecord> findKeysPressedAtTick(int tickNumber) {
        Map<Integer, AnyKeyRecord> keys = new HashMap<>();
        for(TickState tick : replay.tickStates.subList(0, tickNumber)) {
            for (AnyKeyRecord key : tick.getKeys()) {
                switch(key.getAction()) {
                    case 0 -> keys.remove(key.getKey());
                    case 1 -> keys.put(key.getKey(), key);
                }
            }
        }
        return keys;
    }

    private void prepareToStart() {
        pitchTask = LookHelper.changePitchSmoothAsync(replay.getTickState(tickIterator).getPitch(), 120.0f);
        yawTask = LookHelper.changeYawSmoothAsync(replay.getTickState(tickIterator).getYaw(), 120.0f);
        state = ReplayBotState.PREPARING_TO_START;
    }

    private void asyncPlayAlarmSound() {
        CompletableFuture.runAsync(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            assert client.world != null;
            assert client.player != null;
            client.world.playSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.AMBIENT,
                    100.0f,
                    10.0f,
                    false
            );
        });
    }

    private void printDebugInfo() {
        SkyblockBot.LOGGER.info("total packet counter = " + debugPacketCounter);
        SkyblockBot.LOGGER.info("on ground only counter = " + debugOnGroundOnlyCounter);
        SkyblockBot.LOGGER.info("look and on ground counter = " + debugLookAndOnGroundCounter);
        SkyblockBot.LOGGER.info("position and on ground counter = " + debugPositionAndOnGroundCounter);
        SkyblockBot.LOGGER.info("full counter = " + debugFullCounter);
    }

    private void saveRecordingAsync() {
        saveRecordingAsync(ReplayBotSettings.DEFAULT_RECORDING_FILE);
    }

    private void saveRecordingAsync(String filename) {
        CompletableFuture.runAsync(() -> replay.saveToFile(filename));
    }

    public synchronized void pause() {
        if (!state.equals(ReplayBotState.PLAYING)) {
            SkyblockBot.LOGGER.info("Can't pause when not playing");
            return;
        }

        unpressButtons();
        state = ReplayBotState.PAUSED;
        SkyblockBot.LOGGER.info("Paused");
    }

    public synchronized void resume() {
        if (!state.equals(ReplayBotState.PAUSED)) {
            SkyblockBot.LOGGER.info("Can't unpause when not paused");
            return;
        }

        assert MinecraftClient.getInstance().player != null;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double distanceToStartPoint = player.getPos().subtract(replay.getTickState(tickIterator).getPosition()).length();

        if (distanceToStartPoint > ReplayBotSettings.maxDistanceToFirstPoint) {
            SkyblockBot.LOGGER.warn("Can't start " + distanceToStartPoint + " blocks from first point");
            state = ReplayBotState.IDLE;
            return;
        }

        state = ReplayBotState.PLAYING;
        SkyblockBot.LOGGER.info("Unpaused");
    }

    public boolean isExecuting(Task task) {
        return !state.equals(ReplayBotState.IDLE) && replay == task;
    }

    public boolean isPaused() {
        return state.equals(ReplayBotState.PAUSED);
    }

    public boolean isRecording() {
        return state.equals(ReplayBotState.RECORDING);
    }

    public boolean isPlaying() {
        return state.equals(ReplayBotState.PLAYING);
    }
}
