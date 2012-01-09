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
    private static final Key vmGcStatCollectorKey = new Key("collector", false);
    private static final Key vmGcStatRunCountKey = new Key("runtime-count", false);
    private static final Key vmGCstatWallTimeKey = new Key("wall-time", false);

    private static final Key vmMemoryStatVmIdKey = new Key("vm-id", false);
    private static final Key vmMemoryStatTimestampKey = new Key("timestamp", false);
    private static final Key vmMemoryStatAllocatedKey = new Key("allocated", false);
    private static final Key vmMemoryStatFreeKey = new Key("free", false);
    // data structure knows too much about the format of data
    // i would rather not allocate all these keys beforehand
    private static final Key vmMemoryStatEdenKey = new Key("eden", false);
    private static final Key vmMemoryStatS0Key = new Key("s0", false);
    private static final Key vmMemoryStatS1Key = new Key("s1", false);
    private static final Key vmMemoryStatOldKey = new Key("old", false);
    private static final Key vmMemoryStatPermKey = new Key("perm", false);


    private final int vmId;
    private final SystemBackend backend;

    static {
        vmGcStatsCategory.addKey(vmGcStatVmIdKey);
        vmGcStatsCategory.addKey(vmGcStatCollectorKey);
        vmGcStatsCategory.addKey(vmGcStatRunCountKey);
        vmGcStatsCategory.addKey(vmGCstatWallTimeKey);
        vmGcStatsCategory.lock();

        vmMemoryStatsCategory.addKey(vmMemoryStatVmIdKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatTimestampKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatAllocatedKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatFreeKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatEdenKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatS0Key);
        vmMemoryStatsCategory.addKey(vmMemoryStatS1Key);
        vmMemoryStatsCategory.addKey(vmMemoryStatOldKey);
        vmMemoryStatsCategory.addKey(vmMemoryStatPermKey);
        // this lock is invalid
        // vmMemoryStatsCategory.lock();

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
        chunk.put(vmGcStatCollectorKey, vmGcStat.getCollectorName());
        chunk.put(vmGcStatRunCountKey, String.valueOf(vmGcStat.getRunCount()));
        chunk.put(vmGCstatWallTimeKey, String.valueOf(vmGcStat.getWallTime()));

        return chunk;
    }

    private Chunk makeVmMemoryStatChunk(VmMemoryStat vmMemStat) {
        Chunk chunk = new Chunk(vmMemoryStatsCategory, false);
        // FIXME implement this
        return chunk;
    }

}
