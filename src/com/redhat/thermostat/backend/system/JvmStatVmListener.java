package com.redhat.thermostat.backend.system;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.agent.storage.Storage;
import com.redhat.thermostat.common.VmGcStat;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class JvmStatVmListener implements VmListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatVmListener.class);

    private final int vmId;
    private final Storage storage;

    public JvmStatVmListener(Storage storage, int vmId) {
        this.storage = storage;
        this.vmId = vmId;
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
            long timestamp = System.currentTimeMillis();
            JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
            long collectors = extractor.getTotalCollectors();
            for (int i = 0; i < collectors; i++) {
                VmGcStat stat = new VmGcStat(vmId, timestamp,
                        extractor.getCollectorName(i),
                        extractor.getCollectorInvocations(i),
                        extractor.getCollectorTime(i));
                // FIXME storage.addVmGcStat(stat);
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
            // FIXME storage.addVmMemoryStat(stat);
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "error gathering memory info for vm " + vmId, e);
        }
    }

}
