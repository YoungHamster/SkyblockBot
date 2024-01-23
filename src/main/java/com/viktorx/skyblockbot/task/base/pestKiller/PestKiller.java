package com.viktorx.skyblockbot.task.base.pestKiller;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class PestKiller extends BaseTask<PestKillerExecutor> {
    public static final int[][] gardenPlotMap = {{21,13,9,14,22},
                                           {15,5,1,6,16},
                                           {10,2,-1,3,11},
                                           {17,7,4,8,18},
                                           {23,19,12,20,24}};
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
}
