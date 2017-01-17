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

package com.redhat.thermostat.vm.gc.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GcCommonNameMapper {
    
    private static final Map<Set<String>, CollectorCommonName> commonNameMap;
    
    public enum CollectorCommonName {
        /** Serial Collector: {@code -XX:+UseSerialGC} */
        SERIAL_COLLECTOR("Serial Collector"),
        /** G1 Collector: {@code -XX:+UseG1GC} */
        G1("Garbage-First Collector (G1)"),
        /** Parallel Collector: {@code -XX:+UseParallelGC} */
        PARALLEL_COLLECTOR("Parallel Collector"),
        /** Concurrent Mark and Sweep Collector: {@code +XX:+UseConcMarkSweepGC} */
        CONCURRENT_COLLECTOR("Concurrent Collector (Concurrent Mark and Sweep)"),
        /** Mark Sweep Compact Collector: {@code -XX:+UseParNewGC} */
        MARK_SWEEP_COMPACT("Mark Sweep Compact Collector"),
        /** Shenandoah Collector: {@code -XX:+UseShenandoahGC} */
        SHENANDOAH("Shenandoah Collector"),
        UNKNOWN_COLLECTOR("Unknown Collector");
        
        private String humanReadableName;
        
        private CollectorCommonName(String humanReadableName) {
            this.humanReadableName = humanReadableName;
        }
        
        public String getHumanReadableString() {
            return humanReadableName;
        }
    }

    static {
        commonNameMap = new HashMap<>(4);
        // Serial collector has two sub collectors, MSC and Copy
        Set<String> serialCollectorColls = new HashSet<>(2);
        serialCollectorColls.add("MSC");
        serialCollectorColls.add("Copy");
        commonNameMap.put(serialCollectorColls, CollectorCommonName.SERIAL_COLLECTOR);
        // Mark Sweep Compact GC has two sub collectors, MSC and PCopy
        Set<String> msCompactColls = new HashSet<>(2);
        msCompactColls.add("MSC");
        msCompactColls.add("PCopy");
        commonNameMap.put(msCompactColls, CollectorCommonName.MARK_SWEEP_COMPACT);
        // Garbage first collector (G1) has only one collector, namely
        // 'G1 incremental collections'
        Set<String> g1Colls = new HashSet<>(1);
        g1Colls.add("G1 incremental collections");
        commonNameMap.put(g1Colls, CollectorCommonName.G1);
        // Current default, the Parallel Collector has two sub collectors:
        // PSParallelCompact and PSScavenge
        Set<String> parallelColls = new HashSet<>(2);
        parallelColls.add("PSParallelCompact");
        parallelColls.add("PSScavenge");
        commonNameMap.put(parallelColls, CollectorCommonName.PARALLEL_COLLECTOR);
        // Concurrent collector has two sub collectors:
        // CMS and PCopy
        Set<String> concurrentColls = new HashSet<>(2);
        concurrentColls.add("CMS");
        concurrentColls.add("PCopy");
        commonNameMap.put(concurrentColls, CollectorCommonName.CONCURRENT_COLLECTOR);
        // Shenandoah collector has 3 exposed sub collectors even though
        // it's non-generational
        Set<String> shenandoahColls = new HashSet<>(3);
        shenandoahColls.add("Shenandoah concurrent phases");
        shenandoahColls.add("Shenandoah pauses");
        shenandoahColls.add("Shenandoah full GC pauses");
        commonNameMap.put(shenandoahColls, CollectorCommonName.SHENANDOAH);
    }

    /**
     * 
     * @param distinctCollectors
     *            A set of distinct collector names for one JVM.
     * @return The common name of the collector for this set of distinct
     *         collectors.
     */
    public CollectorCommonName mapToCommonName(Set<String> distinctCollectors) {
        for (Set<String> subCollSet: commonNameMap.keySet()) {
            if (subCollSet.equals(distinctCollectors)) {
                return commonNameMap.get(subCollSet);
            }
        }
        // not found so far? return unknown.
        return CollectorCommonName.UNKNOWN_COLLECTOR;
    }

}
