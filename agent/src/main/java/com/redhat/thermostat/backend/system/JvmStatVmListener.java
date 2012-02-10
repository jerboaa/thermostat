/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.backend.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.agent.storage.Category;
import com.redhat.thermostat.agent.storage.Chunk;
import com.redhat.thermostat.agent.storage.Key;
import com.redhat.thermostat.common.VmGcStat;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class JvmStatVmListener implements VmListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatVmListener.class);

    private static final Category vmGcStatsCategory = new Category("vm-gc-stats");
    private static final Category vmMemoryStatsCategory = new Category("vm-memory-stats");

    private static final Key vmGcStatVmIdKey = new Key("vm-id", false);
    private static final Key vmGcStatTimeStampKey = new Key("timestamp", false);
    private static final Key vmGcStatCollectorKey = new Key("collector", false);
    private static final Key vmGcStatRunCountKey = new Key("runtime-count", false);
    /** time in microseconds */
    private static final Key vmGCstatWallTimeKey = new Key("wall-time", false);

    private static final Key vmMemoryStatVmIdKey = new Key("vm-id", false);
    private static final Key vmMemoryStatTimestampKey = new Key("timestamp", false);
    private static final Key vmMemoryStatEdenGenKey = new Key("eden.gen", false);
    private static final Key vmMemoryStatEdenCollectorKey = new Key("eden.collector", false);
    private static final Key vmMemoryStatEdenCapacityKey = new Key("eden.capacity", false);
    private static final Key vmMemoryStatEdenMaxCapacityKey = new Key("eden.max-capacity", false);
    private static final Key vmMemoryStatEdenUsedKey = new Key("eden.used", false);

    private static final Key vmMemoryStatS0GenKey = new Key("s0.gen", false);
    private static final Key vmMemoryStatS0CollectorKey = new Key("s0.collector", false);
    private static final Key vmMemoryStatS0CapacityKey = new Key("s0.capacity", false);
    private static final Key vmMemoryStatS0MaxCapacityKey = new Key("s0.max-capacity", false);
    private static final Key vmMemoryStatS0UsedKey = new Key("s0.used", false);

    private static final Key vmMemoryStatS1GenKey = new Key("s1.gen", false);
    private static final Key vmMemoryStatS1CollectorKey = new Key("s1.collector", false);
    private static final Key vmMemoryStatS1CapacityKey = new Key("s1.capacity", false);
    private static final Key vmMemoryStatS1MaxCapacityKey = new Key("s1.max-capacity", false);
    private static final Key vmMemoryStatS1UsedKey = new Key("s1.used", false);

    private static final Key vmMemoryStatOldGenKey = new Key("old.gen", false);
    private static final Key vmMemoryStatOldCollectorKey = new Key("old.collector", false);
    private static final Key vmMemoryStatOldCapacityKey = new Key("old.capacity", false);
    private static final Key vmMemoryStatOldMaxCapacityKey = new Key("old.max-capacity", false);
    private static final Key vmMemoryStatOldUsedKey = new Key("old.used", false);

    private static final Key vmMemoryStatPermGenKey = new Key("perm.gen", false);
    private static final Key vmMemoryStatPermCollectorKey = new Key("perm.collector", false);
    private static final Key vmMemoryStatPermCapacityKey = new Key("perm.capacity", false);
    private static final Key vmMemoryStatPermMaxCapacityKey = new Key("perm.max-capacity", false);
    private static final Key vmMemoryStatPermUsedKey = new Key("perm.used", false);

    private final int vmId;
    private final SystemBackend backend;

    static {
        vmGcStatsCategory.addKey(vmGcStatVmIdKey);
        vmGcStatsCategory.addKey(vmGcStatTimeStampKey);
        vmGcStatsCategory.addKey(vmGcStatCollectorKey);
        vmGcStatsCategory.addKey(vmGcStatRunCountKey);
        vmGcStatsCategory.addKey(vmGCstatWallTimeKey);
        vmGcStatsCategory.lock();

        vmMemoryStatsCategory.addKey(vmMemoryStatVmIdKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatTimestampKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatEdenGenKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatEdenCollectorKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatEdenCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatEdenMaxCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatEdenUsedKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS0GenKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS0CollectorKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS0CapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS0MaxCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS0UsedKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS1GenKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS1CollectorKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS1CapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS1MaxCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS1UsedKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatOldGenKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatOldCollectorKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatOldCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatOldMaxCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatOldUsedKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatPermGenKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatPermCollectorKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatPermCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatPermMaxCapacityKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatPermUsedKey);

    }

    public JvmStatVmListener(SystemBackend backend, int vmId) {
        this.backend = backend;
        this.vmId = vmId;
    }

    public static Collection<Category> getCategories() {
        ArrayList<Category> categories = new ArrayList<Category>();

        categories.add(vmGcStatsCategory);
        categories.add(vmMemoryStatsCategory);

        return categories;
    }

    @Override
    public void disconnected(VmEvent event) {
        /* nothing to do here */
    }

    @Override
    public void monitorStatusChanged(MonitorStatusChangeEvent event) {
        /* nothing to do here */
    }

    @Override
    public void monitorsUpdated(VmEvent event) {
        MonitoredVm vm = event.getMonitoredVm();
        if (vm == null) {
            throw new NullPointerException();
        }
        recordMemoryStat(vm);
        recordGcStat(vm);
    }

    private void recordGcStat(MonitoredVm vm) {
        try {
            JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
            long collectors = extractor.getTotalCollectors();
            for (int i = 0; i < collectors; i++) {
                long timestamp = System.currentTimeMillis();
                VmGcStat stat = new VmGcStat(vmId, timestamp,
                        extractor.getCollectorName(i),
                        extractor.getCollectorInvocations(i),
                        extractor.getCollectorTime(i));
                backend.store(makeVmGcStatChunk(stat));
            }
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "error gathering gc info for vm " + vmId, e);
        }

    }

    private void recordMemoryStat(MonitoredVm vm) {
        try {
            long timestamp = System.currentTimeMillis();
            JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
            long maxGenerations = extractor.getTotalGcGenerations();
            List<Generation> generations = new ArrayList<Generation>();
            VmMemoryStat stat = new VmMemoryStat(timestamp, vmId, generations);
            for (long generation = 0; generation < maxGenerations; generation++) {
                Generation g = new Generation();
                generations.add(g);
                g.name = extractor.getGenerationName(generation);
                g.capacity = extractor.getGenerationCapacity(generation);
                g.maxCapacity = extractor.getGenerationMaxCapacity(generation);
                try {
                    g.collector = extractor.getGenerationCollector(generation);
                } catch (IllegalArgumentException iae) {
                    /* no collector for this generation */
                    g.collector = Generation.COLLECTOR_NONE;
                }
                long maxSpaces = extractor.getTotalSpaces(generation);
                List<Space> spaces = new ArrayList<Space>();
                g.spaces = spaces;
                for (long space = 0; space < maxSpaces; space++) {
                    Space s = new Space();
                    spaces.add(s);
                    s.index = (int) space;
                    s.name = extractor.getSpaceName(generation, space);
                    s.capacity = extractor.getSpaceCapacity(generation, space);
                    s.maxCapacity = extractor.getSpaceMaxCapacity(generation, space);
                    s.used = extractor.getSpaceUsed(generation, space);
                }
            }
            backend.store(makeVmMemoryStatChunk(stat));
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "error gathering memory info for vm " + vmId, e);
        }
    }

    private Chunk makeVmGcStatChunk(VmGcStat vmGcStat) {
        Chunk chunk = new Chunk(vmGcStatsCategory, false);

        // TODO leave as original data structures
        chunk.put(vmGcStatVmIdKey, String.valueOf(vmGcStat.getVmId()));
        chunk.put(vmGcStatTimeStampKey, String.valueOf(vmGcStat.getTimeStamp()));
        chunk.put(vmGcStatCollectorKey, vmGcStat.getCollectorName());
        chunk.put(vmGcStatRunCountKey, String.valueOf(vmGcStat.getRunCount()));
        chunk.put(vmGCstatWallTimeKey, String.valueOf(vmGcStat.getWallTime()));

        return chunk;
    }

    private Chunk makeVmMemoryStatChunk(VmMemoryStat vmMemStat) {
        Chunk chunk = new Chunk(vmMemoryStatsCategory, false);

        chunk.put(vmMemoryStatVmIdKey, String.valueOf(vmMemStat.getVmId()));
        chunk.put(vmMemoryStatTimestampKey, String.valueOf(vmMemStat.getTimeStamp()));

        Generation newGen = vmMemStat.getGeneration("new");
        Space eden = newGen.getSpace("eden");

        chunk.put(vmMemoryStatEdenGenKey, newGen.name);
        chunk.put(vmMemoryStatEdenCollectorKey, newGen.collector);
        chunk.put(vmMemoryStatEdenCapacityKey, String.valueOf(eden.capacity));
        chunk.put(vmMemoryStatEdenMaxCapacityKey, String.valueOf(eden.maxCapacity));
        chunk.put(vmMemoryStatEdenUsedKey, String.valueOf(eden.used));

        Space s0 = newGen.getSpace("s0");
        chunk.put(vmMemoryStatS0GenKey, newGen.name);
        chunk.put(vmMemoryStatS0CollectorKey, newGen.collector);
        chunk.put(vmMemoryStatS0CapacityKey, String.valueOf(s0.capacity));
        chunk.put(vmMemoryStatS0MaxCapacityKey, String.valueOf(s0.maxCapacity));
        chunk.put(vmMemoryStatS0UsedKey, String.valueOf(s0.used));

        Space s1 = newGen.getSpace("s1");
        chunk.put(vmMemoryStatS1GenKey, newGen.name);
        chunk.put(vmMemoryStatS1CollectorKey, newGen.collector);
        chunk.put(vmMemoryStatS1CapacityKey, String.valueOf(s1.capacity));
        chunk.put(vmMemoryStatS1MaxCapacityKey, String.valueOf(s1.maxCapacity));
        chunk.put(vmMemoryStatS1UsedKey, String.valueOf(s1.used));

        Generation oldGen = vmMemStat.getGeneration("old");
        Space old = oldGen.getSpace("old");

        chunk.put(vmMemoryStatOldGenKey, oldGen.name);
        chunk.put(vmMemoryStatOldCollectorKey, oldGen.collector);
        chunk.put(vmMemoryStatOldCapacityKey, String.valueOf(old.capacity));
        chunk.put(vmMemoryStatOldMaxCapacityKey, String.valueOf(old.maxCapacity));
        chunk.put(vmMemoryStatOldUsedKey, String.valueOf(old.used));

        Generation permGen = vmMemStat.getGeneration("perm");
        Space perm = permGen.getSpace("perm");

        chunk.put(vmMemoryStatPermGenKey, permGen.name);
        chunk.put(vmMemoryStatPermCollectorKey, permGen.collector);
        chunk.put(vmMemoryStatPermCapacityKey, String.valueOf(perm.capacity));
        chunk.put(vmMemoryStatPermMaxCapacityKey, String.valueOf(perm.maxCapacity));
        chunk.put(vmMemoryStatPermUsedKey, String.valueOf(perm.used));

        return chunk;
    }

}
