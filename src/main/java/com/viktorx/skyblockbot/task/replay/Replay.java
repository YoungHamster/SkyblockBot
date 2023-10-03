package com.viktorx.skyblockbot.task.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Replay implements Task {
    private final List<TickState> tickStates = new ArrayList<>();
    private Runnable whenCompleted = null;
    private Runnable whenAborted = null;

    public Replay(){}

    public Replay(String filename) {
        ByteBuffer file;

        try {
            InputStream is = new FileInputStream(filename);
            file = ByteBuffer.wrap(is.readAllBytes());
            is.close();
        } catch (IOException e) {
            SkyblockBot.LOGGER.info("Exception when trying to load from file");
            return;
        }
        tickStates.clear();
        while (file.hasRemaining()) {
            Vec3d pos = new Vec3d(file.getDouble(), file.getDouble(), file.getDouble());
            Vec2f rot = new Vec2f(file.getFloat(), file.getFloat());

            boolean isAttacking = file.get() == 1;
            boolean forward = file.get() == 1;
            boolean backward = file.get() == 1;
            boolean right = file.get() == 1;
            boolean left = file.get() == 1;
            boolean sneak = file.get() == 1;
            boolean sprint = file.get() == 1;
            boolean jump = file.get() == 1;
            int hotbarSlot = file.getInt();
            tickStates.add(new TickState(pos, rot, isAttacking, forward,
                    backward, right, left,
                    sneak, sprint, jump, hotbarSlot)
            );
        }
        SkyblockBot.LOGGER.info("Loaded the recording");
    }

    @Override
    public void execute() {
        ReplayExecutor.INSTANCE.execute(this);
    }

    @Override
    public void saveToFile(String filename) {
        ByteBuffer bb = ByteBuffer.allocate(tickStates.size() * (TickState.getTickStateSize()));

        for (TickState state : tickStates) {
            bb.putDouble(state.getPosition().getX());
            bb.putDouble(state.getPosition().getY());
            bb.putDouble(state.getPosition().getZ());
            bb.putFloat(state.getYaw());
            bb.putFloat(state.getPitch());

            bb.put((byte) (state.isAttack() ? 1 : 0));
            bb.put((byte) (state.isForward() ? 1 : 0));
            bb.put((byte) (state.isBackward() ? 1 : 0));
            bb.put((byte) (state.isRight() ? 1 : 0));
            bb.put((byte) (state.isLeft() ? 1 : 0));
            bb.put((byte) (state.isSneak() ? 1 : 0));
            bb.put((byte) (state.isSprint() ? 1 : 0));
            bb.put((byte) (state.isJump() ? 1 : 0));

            bb.putInt(state.getHotbarSlot());
        }
        try {
            OutputStream os = new FileOutputStream(filename, false);

            os.write(bb.array());
            os.close();

            assert MinecraftClient.getInstance().player != null;
            SkyblockBot.LOGGER.info("Saved the recording");
        } catch (IOException e) {
            SkyblockBot.LOGGER.info("Exception when trying to save movement recording to a file");
        }
    }

    @Override
    public void completed() {
        if(whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
    }

    @Override
    public void aborted() {
        if(whenAborted != null)
            CompletableFuture.runAsync(whenAborted);
    }

    @Override
    public void whenCompleted(Runnable whenCompleted) {
        this.whenCompleted = whenCompleted;
    }

    @Override
    public void whenAborted(Runnable whenAborted) {
        this.whenAborted = whenAborted;
    }

    public void addTickState(TickState newState) {
        tickStates.add(newState);
    }

    public TickState getTickState(int tickIterator) {
        return tickStates.get(tickIterator);
    }

    public int size() {
        return tickStates.size();
    }

    /* DANGEROUS */
    public  void deleteFromIndexToIndex(int start, int end) {
        tickStates.subList(start + 1, end).clear();
    }
}
