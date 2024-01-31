package com.viktorx.skyblockbot.task.base.pestKiller;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

/**
 * This may be considered a compound task, because it depends on Replay task for searching pest inside given plot,
 * but i'll count it as a base task, because its architecture is same as other base tasks
 */
public class PestKiller extends BaseTask<PestKillerExecutor> {
    public static final int[][] gardenPlotMap = {{21, 13, 9, 14, 22},
            {15, 5, 1, 6, 16},
            {10, 2, -1, 3, 11},
            {17, 7, 4, 8, 18},
            {23, 19, 12, 20, 24}};
    public static final int plotsize = 96;
    public static final int maxPlotY = 77;
    public static final int minPlotY = 67;
    private final String pestName;
    private final int plotNumber;

    public PestKiller(String pestName, int plotNumber, Runnable whenCompleted, Runnable whenAborted) {
        super(PestKillerExecutor.INSTANCE, whenCompleted, whenAborted);
        this.pestName = pestName;
        this.plotNumber = plotNumber;
    }

    public String getPestName() {
        return pestName;
    }

    public int getPlotNumber() {
        return plotNumber;
    }

    public boolean isPosInsidePlot(Vec3d pos) {
        Vec3i min = getMinPlotCoords();
        return pos.x >= min.getX() && pos.x <= min.getX() + plotsize &&
                pos.y >= minPlotY && pos.y <= maxPlotY &&
                pos.z >= min.getZ() && pos.z <= min.getZ() + plotsize;
    }

    public Vec3i getMinPlotCoords() {
        int minx = -1;
        int minz = -1;

        // TODO - get rid of this loop
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (PestKiller.gardenPlotMap[i][j] == plotNumber) {
                    minz = (int) ((i - 2.5) * plotsize);
                    minx = (int) ((j - 2.5) * plotsize);

                    if (GlobalExecutorInfo.debugMode.get()) {
                        SkyblockBot.LOGGER.info("Found plots min x and z. i = " + i + ", j = " + j);
                    }
                }
            }
        }

        return new Vec3i(minx, minPlotY, minz);
    }

    public double findFreeHeight() {
        Vec3i plotMinPoint = getMinPlotCoords();
        int minx = plotMinPoint.getX();
        int miny = plotMinPoint.getY();
        int minz = plotMinPoint.getZ();


        if (GlobalExecutorInfo.debugMode.get()) {
            SkyblockBot.LOGGER.info("minX = " + plotMinPoint.getX() + ", minZ = " + plotMinPoint.getZ());
        }

        boolean goToNextY = false;
        for (int y = miny; y < maxPlotY; y++) {

            for (int x = minx; (x < minx + plotsize) && !goToNextY; x++) {

                for (int z = minz; (z < minz + plotsize) && !goToNextY; z++) {

                    if (Utils.isBlockSolid(new BlockPos(x, y, z))) {
                        goToNextY = true;
                    }
                }
            }
            if (!goToNextY) {
                return y;
            }
            goToNextY = false;
        }

        return -1;
    }
}
