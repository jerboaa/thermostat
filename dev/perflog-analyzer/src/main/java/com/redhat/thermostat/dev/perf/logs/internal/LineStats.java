package com.redhat.thermostat.dev.perf.logs.internal;

import java.io.PrintStream;
import java.util.List;

import com.redhat.thermostat.dev.perf.logs.StatsConfig;

/**
 * Common interface for a collection of log file stats.
 *
 * @param <T> The type of the stat entries.
 */
public interface LineStats<T extends LineStat> {

    void setSharedStatementState(SharedStatementState sharedState);
    
    void setStats(List<T> stats);
    
    void setConfig(StatsConfig config);
    
    void printSummary(PrintStream out);
}
