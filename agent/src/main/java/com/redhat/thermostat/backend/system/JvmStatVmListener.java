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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.common.dao.VmGcStatConverter;
import com.redhat.thermostat.common.dao.VmMemoryStatConverter;
import com.redhat.thermostat.common.model.VmGcStat;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class JvmStatVmListener implements VmListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatVmListener.class);

    private final int vmId;
    private final SystemBackend backend;

    public JvmStatVmListener(SystemBackend backend, int vmId) {
        this.backend = backend;
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
            JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
            long collectors = extractor.getTotalCollectors();
            for (int i = 0; i < collectors; i++) {
                long timestamp = System.currentTimeMillis();
                VmGcStat stat = new VmGcStat(vmId, timestamp,
                        extractor.getCollectorName(i),
                        extractor.getCollectorInvocations(i),
                        extractor.getCollectorTime(i));
                backend.store(new VmGcStatConverter().vmGcStatToChunk(stat));
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
            backend.store(new VmMemoryStatConverter().vmMemoryStatToChunk(stat));
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "error gathering memory info for vm " + vmId, e);
        }
    }


}
