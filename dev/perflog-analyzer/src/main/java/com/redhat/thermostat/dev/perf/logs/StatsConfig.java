package com.redhat.thermostat.dev.perf.logs;

import java.io.File;

public class StatsConfig {

    public static final String SORT_KEY = "sort-by";
    public static final String DIRECTION_KEY = "direction";
    public static final String SHOW_BACKING = "show-backing";

    private final boolean showBacking;
    private final SortBy sortBy;
    private final Direction direction;
    private final File logFile;
    
    public StatsConfig(File logFile, SortBy sortBy, Direction direction, boolean showBacking) {
        this.sortBy = sortBy;
        this.direction = direction;
        this.logFile = logFile;
        this.showBacking = showBacking;
    }

    public File getLogFile() {
        return logFile;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isShowBacking() {
        return showBacking;
    }
    
}
