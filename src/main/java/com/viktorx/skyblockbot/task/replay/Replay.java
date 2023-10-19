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

public class Replay extends Task {
    private final List<TickState> tickStates = new ArrayList<>();

    private static final long saveProtocolVersion = 696969;

    public Replay(){}

    public Replay(String filename) {
        loadFromFile(filename);
    }

    public void execute() {
        try {
            ReplayExecutor.INSTANCE.execute(this);
        } catch (InterruptedException e) {
            SkyblockBot.LOGGER.info("Interrupted while trying to execute Replay, wtf?");
        }
    }

    public void pause() {
        ReplayExecutor.INSTANCE.pause();
    }

    public void resume() {
        ReplayExecutor.INSTANCE.resume();
    }

    public void abort() {
        ReplayExecutor.INSTANCE.abort();
    }

    public void saveToFile(String filename) {
        ByteBuffer bb = ByteBuffer.allocate(8 + tickStates.size() * (TickState.getTickStateSize()));

        bb.putLong(saveProtocolVersion); // some unique number to represent save protocol version

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

    public boolean isExecuting() {
        return ReplayExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return ReplayExecutor.INSTANCE.isPaused();
    }

    private void loadFromFile(String filename) {
        ByteBuffer file;

        try {
            InputStream is = new FileInputStream(filename);
            file = ByteBuffer.wrap(is.readAllBytes());
            is.close();
        } catch (IOException e) {
            SkyblockBot.LOGGER.info("Exception when trying to load from file");
            return;
        }

        if(!file.hasRemaining()) {
            SkyblockBot.LOGGER.info("Can't load the recording, it's empty");
            return;
        }

        if(file.getLong() != saveProtocolVersion) {
            loadFromFileLegacy(file.rewind());
            return;
        }

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

    private void loadFromFileLegacy(ByteBuffer file) {
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
            tickStates.add(new TickState(pos, rot, isAttacking, forward,
                    backward, right, left, sneak, sprint, jump, -1)
            );
        }
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
