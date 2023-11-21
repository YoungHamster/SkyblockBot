package com.viktorx.skyblockbot.task.base.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.replay.tickState.AnyKeyRecord;
import com.viktorx.skyblockbot.task.base.replay.tickState.TickState;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;
import com.viktorx.skyblockbot.utils.Utils;
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

public class ReplayExecutor extends BaseExecutor {

    public static final ReplayExecutor INSTANCE = new ReplayExecutor();
    private final List<String> itemsWhenStarted = new ArrayList<>();
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

    public void Init() {

        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    protected void setReplay(Replay replay) {
        this.task = replay;
        this.replay = replay;
        tickIterator = 0;
    }

    public synchronized void startRecording() {
        if (!state.getClass().equals(Idle.class)) {
            SkyblockBot.LOGGER.info("Can't start recording when state = " + state.getClass().getSimpleName());
            return;
        }

        SkyblockBot.LOGGER.info("started recording");
        replay = new Replay();

        debugPacketCounter = 0;
        debugOnGroundOnlyCounter = 0;
        debugLookAndOnGroundCounter = 0;
        debugPositionAndOnGroundCounter = 0;
        debugFullCounter = 0;

        state = new Recording();
    }

    public synchronized void stopRecording() {
        SkyblockBot.LOGGER.info("stopped recording");

        if (GlobalExecutorInfo.debugMode.get()) {
            printDebugInfo();
        }

        saveRecordingAsync();

        state = new Idle();
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    @Override
    public synchronized <T extends BaseTask<?>> ExecutorState execute(T task) {
        if (!state.getClass().equals(Idle.class)) {
            SkyblockBot.LOGGER.warn("Can't play while state = " + state.getClass().getSimpleName());
            return new Idle();
        }

        this.task = task;
        this.replay = (Replay) task;

        if (this.replay.size() == 0) {
            SkyblockBot.LOGGER.warn("can't start playing, nothing to play");
            abort();
            return new Idle();
        }

        assert MinecraftClient.getInstance().player != null;

        /*
         * If tick 0 from replay isn't close to player's current position we attempt to find replay tick close enough
         * to current position
         * If we do we can just start for the middle of the replay instead of refusing to work
         */
        tickIterator = 0;
        if (!isPlayerInCorrectPosition()) {
            if (tryStartingFromMiddleOfRecording(this.replay)) {
                return new Idle();
            }
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

        state = new PreparingToStart();
        return state;
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        SkyblockBot.LOGGER.error("ReplayExecutor.whenExecute() was called! That is not supposed to happen");
        return null;
    }

    /**
     * @return True if can't start from the middle, False if can
     */
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
            currentlyPressedKeys.putAll(keys);

            SkyblockBot.LOGGER.info("Found tick to start from! Continuing replay from " + lowestDistanceIterator + " tick state");
        } else {
            SkyblockBot.LOGGER.warn(
                    "Can't find fitting point. Distance to first point: " + getDistanceToExpectedPosition());
            state = new Idle();
            abort();
            return true;
        }
        return false;
    }

    @Override
    public synchronized void abort() {
        unpressButtons();
        tickIterator = 0;

        SkyblockBot.LOGGER.info("aborted playing");
        state = new Idle();

        printDebugInfo();

        task.aborted();
    }

    protected boolean isPlayerInCorrectPosition() {
        return getDistanceToExpectedPosition() < ReplayBotSettings.maxDistanceToFirstPoint;
    }

    private double getDistanceToExpectedPosition() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Vec3d expected = replay.getTickState(tickIterator).getPosition();
        assert player != null;
        Vec3d actual = player.getPos();

        return actual.subtract(expected).length();
    }

    public void onKeyPress(AnyKeyRecord newKey) {
        if (state.getClass().equals(Recording.class)) {
            ((Recording) state).newKeys.add(newKey);
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
                    if (!currentlyPressedKeys.containsKey(key.getKey())) {
                        currentlyPressedKeys.put(key.getKey(), key);
                        key.press();
                    }
                }

                case 2 -> key.press();

                default -> SkyblockBot.LOGGER.warn("Key action is not 0, 1 or 2, its: " + key.getAction());
            }
        }
    }

    private Map<Integer, AnyKeyRecord> findKeysPressedAtTick(int tickNumber) {
        Map<Integer, AnyKeyRecord> keys = new HashMap<>();
        for (TickState tick : replay.tickStates.subList(0, tickNumber)) {
            for (AnyKeyRecord key : tick.getKeys()) {
                switch (key.getAction()) {
                    case 0 -> keys.remove(key.getKey());
                    case 1 -> keys.put(key.getKey(), key);
                }
            }
        }
        return keys;
    }

    private void printDebugInfo() {
        if (GlobalExecutorInfo.debugMode.get()) {
            SkyblockBot.LOGGER.info("total packet counter = " + debugPacketCounter);
            SkyblockBot.LOGGER.info("on ground only counter = " + debugOnGroundOnlyCounter);
            SkyblockBot.LOGGER.info("look and on ground counter = " + debugLookAndOnGroundCounter);
            SkyblockBot.LOGGER.info("position and on ground counter = " + debugPositionAndOnGroundCounter);
            SkyblockBot.LOGGER.info("full counter = " + debugFullCounter);
        }
    }

    private void saveRecordingAsync() {
        saveRecordingAsync(ReplayBotSettings.DEFAULT_RECORDING_FILE);
    }

    private void saveRecordingAsync(String filename) {
        CompletableFuture.runAsync(() -> replay.saveToFile(filename));
    }

    @Override
    public synchronized void pause() {
        if (!state.getClass().equals(Playing.class)) {
            SkyblockBot.LOGGER.info("Can't pause when not playing");
            return;
        }

        unpressButtons();
        state = new Paused();
        SkyblockBot.LOGGER.info("Paused");
    }

    @Override
    public synchronized void resume() {
        if (!state.getClass().equals(Paused.class)) {
            SkyblockBot.LOGGER.info("Can't unpause when not paused");
            return;
        }

        assert MinecraftClient.getInstance().player != null;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double distanceToStartPoint = player.getPos().subtract(replay.getTickState(tickIterator).getPosition()).length();

        if (distanceToStartPoint > ReplayBotSettings.maxDistanceToFirstPoint) {
            SkyblockBot.LOGGER.warn("Can't start " + distanceToStartPoint + " blocks from first point");
            state = new Idle();
            return;
        }

        state = new Playing();
        SkyblockBot.LOGGER.info("Unpaused");
    }

    public boolean isRecording() {
        return state.getClass().equals(Recording.class);
    }

    public boolean isPlaying() {
        return state.getClass().equals(Playing.class);
    }

    protected static class Recording implements ExecutorState {
        private final ReplayExecutor parent = ReplayExecutor.INSTANCE;
        private final Queue<AnyKeyRecord> newKeys = new ArrayBlockingQueue<>(15);

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            if (!detectAndCorrectLagBack(client.player)) {
                return new Idle();
            }
            parent.replay.addTickState(getCurrentTickState());
            return this;
        }

        /**
         * Used for recording only
         *
         * @return true - if lagback was corrected and we can continue recording
         * false - if lagback can't be corrected and recording has to stop
         */
        private boolean detectAndCorrectLagBack(@NotNull ClientPlayerEntity player) {
            if (parent.serverChangedPositionRotation) {
                parent.serverChangedPositionRotation = false;

                int bestFit = -1;
                double bestFitDistance = ReplayBotSettings.reactToLagbackThreshold;

                for (int i = 0; i < ReplayBotSettings.maxLagbackTicksWhenRecording && parent.replay.size() > i; i++) {
                    int index = parent.replay.size() - i - 1;

                    double distance = Utils.distanceBetween(
                            player.getPos(),
                            parent.replay.getTickState(index).getPosition()
                    );

                    if (distance < bestFitDistance) {
                        bestFit = index;
                        bestFitDistance = distance;
                    }
                }

                if (bestFit != -1) {
                    parent.replay.deleteFromIndexToIndex(bestFit + 1, parent.replay.size());
                    SkyblockBot.LOGGER.info("Lagged back when recording, corrected! Best fit distance = " + bestFitDistance);
                } else {
                    SkyblockBot.LOGGER.warn("Lagged back when recording, can't correct, stopping the recording");
                    parent.stopRecording();
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
    }

    protected static class AntiDetectTriggered implements ExecutorState {
        private final ReplayExecutor parent = ReplayExecutor.INSTANCE;
        private int antiDetectTriggeredTickCounter = 0;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (antiDetectTriggeredTickCounter++ == ReplayBotSettings.antiDetectTriggeredWaitTicks ||
                    parent.tickIterator == 0 || parent.tickIterator == parent.replay.size() ||
                    client.currentScreen != null) {
                parent.abort();
                antiDetectDone();
                return new Idle();
            }

            asyncPlayAlarmSound();
            parent.pressButtons(parent.replay.getTickState(parent.tickIterator));

            Vec2f prevRot = parent.replay.getTickState(parent.tickIterator - 1).getRotation();
            Vec2f currentRot = parent.replay.getTickState(parent.tickIterator).getRotation();
            Vec2f deltaCam = currentRot.add(prevRot.multiply(-1));

            assert client.player != null;
            client.player.setYaw(client.player.getYaw() + deltaCam.x);
            client.player.setPitch(client.player.getPitch() + deltaCam.y);

            parent.tickIterator++;
            return this;
        }

        private void antiDetectDone() {
            TGBotDaemon.INSTANCE.takeAndSendScreenshot("Anti detect triggered!!!", true);

            parent.unpressButtons();
            if (ReplayBotSettings.autoQuitWhenAntiDetect) {
                MinecraftClient.getInstance().stop();
            }
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
    }

    protected class PreparingToStart implements ExecutorState {
        private final CompletableFuture<Void> yawTask;
        private final CompletableFuture<Void> pitchTask;
        private final CompletableFuture<Void> stopFlyingTask;

        public PreparingToStart() {
            pitchTask = LookHelper.changePitchSmoothAsync(replay.getTickState(tickIterator).getPitch());
            yawTask = LookHelper.changeYawSmoothAsync(replay.getTickState(tickIterator).getYaw());

            /*
             * Turns out if you drink mushroom soup you always spawn in the garden in flight, so to account for it a have to land before doing anything
             */
            stopFlyingTask = CompletableFuture.runAsync(() -> {
                assert MinecraftClient.getInstance().player != null;
                if (!MinecraftClient.getInstance().player.isOnGround()) {
                    MinecraftClient.getInstance().options.sneakKey.setPressed(true);

                    while (!MinecraftClient.getInstance().player.isOnGround()) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    MinecraftClient.getInstance().options.sneakKey.setPressed(false);
                }
            });
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (yawTask.isDone() && pitchTask.isDone() && stopFlyingTask.isDone()) {
                /*
                 * this is supposed to start pressing keys if we start from the middle of the recording
                 * and there are already some keys pressed
                 */
                currentlyPressedKeys.forEach((key, value) -> value.firstPress());

                return new Playing();

            }
            return this;
        }
    }

    protected class Playing implements ExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (GlobalExecutorInfo.worldLoading.get()) {
                abort();
                return new Idle();
            }

            if (client.currentScreen != null) {
                SkyblockBot.LOGGER.warn("currentScreen isn't null!!! ReplayExecutor waiting until it's null again");
                return this;
            }

            if (antiDetect(client)) {
                TGBotDaemon.INSTANCE.takeAndSendScreenshot("Anti detect triggered!!!", true);
                return new AntiDetectTriggered();
            }

            TickState tickState = replay.getTickState(tickIterator);

            if (replay.isRelative()) {
                if (tickIterator > 0) {
                    tickState.setRotationRelative(client, replay.getTickState(tickIterator - 1));
                }
                tickState.setPositionRelative(client);
            } else {
                tickState.setRotationForClient(client);
                tickState.setPositionForClient(client);
            }
            pressButtons(tickState);

            tickIterator++;
            if (tickIterator == replay.size()) {
                tickIterator = 0;

                printDebugInfo();
                unpressButtons();
                replay.completed();
                return new Idle();
            }
            return this;
        }

        /**
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
            for (int i = 0; i < ReplayBotSettings.checkForCollisionsAdvanceTicks && tickIterator + i < replay.size(); i++) {

                /*
                 * I want to round the Y correctly
                 */
                Vec3d pos = replay.getTickState(tickIterator + i).getPosition();
                pos = new Vec3d(Math.floor(pos.x), Math.rint(pos.y), Math.floor(pos.z));

                BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
                BlockPos above = new BlockPos(blockPos).up();

                if (client.world == null) {
                    SkyblockBot.LOGGER.error("client.world == null wtf???????? " +
                            "Can't check if next state is possible. Returning true as default.");
                    return true;
                }

                boolean isBlockSolid = Utils.isBlockSolid(blockPos);
                boolean isBlockAboveSolid = Utils.isBlockSolid(above);

                if (isBlockSolid || isBlockAboveSolid) {
                    String blockName = client.world.getBlockState(blockPos).getBlock().asItem().toString();
                    String blockAboveName = client.world.getBlockState(above).getBlock().asItem().toString();

                    SkyblockBot.LOGGER.info(
                            "Block at " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ()
                                    + " : " + blockName + " solid-" + isBlockSolid
                                    + "\nBlock above: " + blockAboveName + " solid-" + isBlockAboveSolid);

                    return false;
                }
            }
            return true;
        }

        /**
         * Corrects position
         * return - true if it is correct or was corrected (correct by changing tickIterator to index of state closest to current state)
         * false if the position can't be corrected(which means player was teleported to check for macros or lagged back too far)
         */
        private boolean checkPosAdjustLag(@NotNull ClientPlayerEntity player) {
            TickState state = replay.getTickState(tickIterator - 1);
            double deltaExpectedPos = Utils.distanceBetween(player.getPos(), state.getPosition());
            if (deltaExpectedPos > ReplayBotSettings.reactToLagbackThreshold) {
                // if delta is too big something is wrong, check if we were simply lagged back and if not-stop
                double minDelta = ReplayBotSettings.reactToLagbackThreshold;
                int bestTickIndex = -1;

                for (int i = 1; i <= ReplayBotSettings.maxLagbackTicks && tickIterator >= i; i++) {
                    double delta = Utils.distanceBetween(
                            player.getPos(),
                            replay.getTickState(tickIterator - i).getPosition());

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
                    tickIterator = bestTickIndex;


                    /*
                     * Figure out which keys are pressed at the tick we lagged back to, press them before continuing
                     * Also unpress keys that weren't pressed at that tick
                     */
                    Map<Integer, AnyKeyRecord> keys = findKeysPressedAtTick(tickIterator);

                    keys.forEach((key, value) -> {
                        if (!currentlyPressedKeys.containsKey(key)) {
                            currentlyPressedKeys.put(key, value);
                            value.firstPress();
                        }
                    });

                    List<Integer> keysToRemove = new ArrayList<>();
                    currentlyPressedKeys.forEach((key, value) -> {
                        if (!keys.containsKey(key)) {
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
    }
}
