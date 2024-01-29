package com.viktorx.skyblockbot.task.base.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.replay.tickState.AnyKeyRecord;
import com.viktorx.skyblockbot.task.base.replay.tickState.KeyboardKeyRecord;
import com.viktorx.skyblockbot.task.base.replay.tickState.MouseKeyRecord;
import com.viktorx.skyblockbot.task.base.replay.tickState.TickState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Replay extends BaseTask<ReplayExecutor> {
    private static final long saveProtocolVersion = 42070;
    protected final List<TickState> tickStates = new ArrayList<>();
    private final boolean relativeMode;
    private Vec2f startRot;
    private Vec3d startPos;

    public Replay() {
        super(ReplayExecutor.INSTANCE, null, null);
        relativeMode = false;
    }

    public Replay(String filename, Runnable whenCompleted, Runnable whenAborted) {
        super(ReplayExecutor.INSTANCE, whenCompleted, whenAborted);
        this.relativeMode = false;
        loadFromFile(filename);
    }

    public Replay(boolean relativeMode, String filename, Runnable whenCompleted, Runnable whenAborted) {
        super(ReplayExecutor.INSTANCE, whenCompleted, whenAborted);
        this.relativeMode = relativeMode;
        loadFromFile(filename);
    }

    public void setStartRotPos(Vec2f startRot, Vec3d startPos) {
        this.startRot = startRot;
        this.startPos = startPos;
    }

    public boolean isRelative() {
        return relativeMode;
    }

    public void saveToFile(String filename) {
        /*
         * With varying tickState size we have to calculate buffer size tick by tick
         */
        int bufferSize = 8 + tickStates.size() * TickState.getEmptyTickStateSize();
        for (TickState tick : tickStates) {
            for (AnyKeyRecord key : tick.getKeys()) {
                bufferSize += key.getSize();
            }
        }

        ByteBuffer bb = ByteBuffer.allocate(bufferSize);

        bb.putLong(saveProtocolVersion); // some unique number to represent save protocol version

        for (TickState state : tickStates) {
            bb.putDouble(state.getPosition().getX());
            bb.putDouble(state.getPosition().getY());
            bb.putDouble(state.getPosition().getZ());
            bb.putFloat(state.getYaw());
            bb.putFloat(state.getPitch());

            bb.putInt(state.getKeys().size());

            for (AnyKeyRecord key : state.getKeys()) {
                bb.put(key.getData());
            }
        }
        try {
            File f = new File("replays");
            if (!f.isDirectory()) {
                if(!f.mkdir()) {
                    SkyblockBot.LOGGER.error("Can't create \"replays\" to save the replay!");
                    return;
                }
            }
            OutputStream os = new FileOutputStream("replays\\\\" + filename, false);

            os.write(bb.array());
            os.close();

            assert MinecraftClient.getInstance().player != null;
            SkyblockBot.LOGGER.info("Saved the recording");
        } catch (IOException e) {
            SkyblockBot.LOGGER.info("Exception when trying to save movement recording to a file");
        }
    }

    public void loadFromFile(String filename) {
        ByteBuffer file;

        try {
            InputStream is = new FileInputStream("replays\\\\" + filename);
            file = ByteBuffer.wrap(is.readAllBytes());
            is.close();
        } catch (IOException e) {
            SkyblockBot.LOGGER.info("Exception when trying to load replay from " + filename);
            return;
        }

        if (!file.hasRemaining()) {
            SkyblockBot.LOGGER.info("Can't load the recording, it's empty");
            return;
        }

        tickStates.clear();

        long recordingVersion = file.getLong();
        if (recordingVersion != saveProtocolVersion) {
            SkyblockBot.LOGGER.info("Recording ver: " + recordingVersion + ", save ver: " + saveProtocolVersion);
            SkyblockBot.LOGGER.info("Recording file is deprecated, it is no longer supported, make new recording");
            return;
        }

        while (file.hasRemaining()) {
            Vec3d pos = new Vec3d(file.getDouble(), file.getDouble(), file.getDouble());
            Vec2f rot = new Vec2f(file.getFloat(), file.getFloat());

            int numberOfKeys = file.getInt();
            List<AnyKeyRecord> keys = new ArrayList<>();

            for (int i = 0; i < numberOfKeys; i++) {
                byte keyType = file.get();

                if (keyType == KeyboardKeyRecord.getType()) {
                    keys.add(new KeyboardKeyRecord(file.getInt(), file.getInt(), file.getInt(), file.getInt()));
                } else if (keyType == MouseKeyRecord.getType()) {
                    keys.add(new MouseKeyRecord(file.getInt(), file.getInt(), file.getInt()));
                } else {
                    SkyblockBot.LOGGER.error("Illegal key type! " + keyType);
                    tickStates.clear();
                    return;
                }
            }

            tickStates.add(new TickState(pos, rot, keys)
            );
        }
        SkyblockBot.LOGGER.info("Loaded the recording");
    }

    public void addTickState(TickState newState) {
        tickStates.add(newState);
    }

    public TickState getTickState(int tickIterator) {
        return tickStates.get(tickIterator);
    }

    public Vec3d getTickPos(int tickIterator) {
        if(relativeMode) {
            return tickStates.get(tickIterator).getPositionRelative(tickStates.get(0), startRot, startPos);
        } else {
            return tickStates.get(tickIterator).getPosition();
        }
    }

    public Vec2f getTickRot(int tickIterator) {
        if(relativeMode) {
            return tickStates.get(tickIterator).getRotationRelative(tickStates.get(0), startRot);
        } else {
            return tickStates.get(tickIterator).getRotation();
        }
    }

    public int size() {
        return tickStates.size();
    }

    /* DANGEROUS */
    public void deleteFromIndexToIndex(int start, int end) {
        tickStates.subList(start + 1, end).clear();
    }

    public boolean isPlayerInStartingPosition() {
        ReplayExecutor.INSTANCE.setReplay(this);
        return ReplayExecutor.INSTANCE.isPlayerInCorrectPosition();
    }
}
