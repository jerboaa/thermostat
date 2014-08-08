package com.redhat.thermostat.dev.perf.logs.internal;


/**
 * A filter by which {@link LineStat} get categorized into buckets.
 * 
 */
interface LineStatsFilter<S extends LineStat, T extends LineStats<S>> {

    /**
     * 
     * @param stat
     * @return true if and only if the filter matches for the given stat.
     */
    public boolean matches(LineStat stat);

    /**
     * 
     * @return The name of the bucket to which to add matching {@link LineStat}
     *         elements to.
     */
    public String getBucketName();
    
    /**
     * 
     * @return The stats class pertaining to the collection of filtered stats.
     * 
     * @see QueueStats
     * @see StatementStats
     */
    public Class<T> getStatsClass();
    
}
