/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.dev.perf.logs.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.dev.perf.logs.StatsConfig;

class LogFileStats {

    private final StatsConfig config;
    private final List<LineStat> allStats;
    private final Map<String, List<?>> bucket;
    private final SharedStatementState state;
    private final Map<String, LineStatsFilter<?, ?>> statFiltersMap;
    private final List<LineStatsFilter<?, ?>> statFilterList;
    
    LogFileStats(SharedStatementState state, StatsConfig config) {
        this.config = config;
        this.allStats = new ArrayList<>();
        this.bucket = new HashMap<>();
        this.statFiltersMap = new HashMap<>();
        this.statFilterList = new ArrayList<>();
        this.state = state;
    }
    
    /**
     * Adds the stat to the bucket of {@link LineStat} elements for which it
     * matches registered filters.
     * 
     * @param stat
     *            The element which should get added to the bucket.
     */
    <S extends LineStat, T extends LineStats<S>> void add(LineStat stat) {
        allStats.add(stat);
        LineStatsFilter<S, T> filter = findMatchingFilter(stat);
        if (filter == null) {
            // no matching filter
            return;
        }
        String name = filter.getBucketName();
        List<S> bucketList = getBucketList(name);
        @SuppressWarnings("unchecked") // if the filter matches this must work
        S upCastedStat = (S)stat;
        bucketList.add(upCastedStat);
    }
    
    private <S extends LineStat> List<S> getBucketList(String name) {
        @SuppressWarnings("unchecked")
        List<S> list = (List<S>)bucket.get(name);
        if (list == null) {
            // no such bucket list, create it.
            List<S> bucketList = new ArrayList<>();
            bucket.put(name, bucketList);
            list = bucketList;
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private <S extends LineStat, T extends LineStats<S>> LineStatsFilter<S, T> findMatchingFilter(LineStat stat) {
        for (LineStatsFilter<?, ?> filter: statFilterList) {
            if (filter.matches(stat)) {
                return (LineStatsFilter<S, T>)filter;
            }
        }
        return null;
    }

    /**
     * Registers the given filter. The number of registered filters
     * determines the number of buckets. Filters must be registered before
     * stats get added. Subsequent calls to {@link #add(LineStat)} will add the
     * line stat to the appropriate bucket which matches a registered filter.
     * 
     * @param filter The filter which should get registered.
     */
    <S extends LineStat, T extends LineStats<S>> void registerStatsFilter(final LineStatsFilter<S, T> filter) throws IllegalFilterException {
        if (statFiltersMap.containsKey(filter.getBucketName())) {
            throw new IllegalFilterException("bucket name '" + filter.getBucketName() + "' already taken");
        }
        statFilterList.add(filter);
        statFiltersMap.put(filter.getBucketName(), filter);
    }
    
    /**
     * 
     * @param filter The filter used when {@link #registerStatsFilter(LineStatsFilter)} was called.
     * @return T An instance of the stats class populated with filtered stats.
     */
    <S extends LineStat, T extends LineStats<S>> T getStatsForBucket(LineStatsFilter<S, T> filter) {
        @SuppressWarnings("unchecked")
        List<S> stats = (List<S>)bucket.get(filter.getBucketName());
        LineStatsFactory<S, T> factory = new LineStatsFactory<>(state, config, stats, filter.getStatsClass());
        return factory.create();
    }
    
    int getTotalStats() {
        return allStats.size();
    }
    
    /**
     * 
     * @return The list of filters in the order they were registered.
     */
    List<LineStatsFilter<?, ?>> getRegisteredFilters() {
        return statFilterList;
    }
}
