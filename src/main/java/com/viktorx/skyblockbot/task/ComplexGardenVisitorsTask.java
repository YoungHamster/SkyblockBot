package com.viktorx.skyblockbot.task;

public class ComplexGardenVisitorsTask extends Task {

    private Task goToVisitors;
    private Task goBackToFarm;
    private Task getVisitorDesiredItems;
    private Task buyBZItem;
    private Task giveVisitorItems;

    public void execute() {
    }
    public void pause() {
    }
    public void resume() {
    }
    public void abort() {
    }
    public boolean isExecuting() {
        return true;
    }
    public boolean isPaused() {
        return true;
    }
}
