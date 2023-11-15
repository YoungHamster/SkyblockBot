package com.viktorx.skyblockbot.task.base.composter.getInfo;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class GetComposterInfo extends BaseTask<GetComposterInfoExecutor> {
    private int maxOrganicMatter;
    private int maxFuel;

    public GetComposterInfo(Runnable whenCompleted, Runnable whenAborted) {
        super(GetComposterInfoExecutor.INSTANCE, whenCompleted, whenAborted);
    }

    public int getMaxOrganicMatter() {
        return maxOrganicMatter;
    }

    public void setMaxOrganicMatter(int maxOrganicMatter) {
        this.maxOrganicMatter = maxOrganicMatter;
    }

    public int getMaxFuel() {
        return maxFuel;
    }

    public void setMaxFuel(int maxFuel) {
        this.maxFuel = maxFuel;
    }

    public String getComposterMenuName() {
        return "Composter";
    }
}
