/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.agent.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.VmTlabStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.vm.memory.common.model.VmTlabStat;

public class VmMemoryVmListener implements VmUpdateListener {
    
    private static final Logger logger = LoggingUtils.getLogger(VmMemoryVmListener.class);

    private final String vmId;
    private final VmMemoryStatDAO memDAO;
    private final VmTlabStatDAO tlabDAO;
    private final String writerId;
    private final Clock clock;
    
    private boolean error;

    public VmMemoryVmListener(String writerId, VmMemoryStatDAO vmMemoryStatDao, VmTlabStatDAO vmTlabStatDao, String vmId) {
        this(writerId, vmMemoryStatDao, vmTlabStatDao, new SystemClock(), vmId);
    }

    public VmMemoryVmListener(String writerId, VmMemoryStatDAO vmMemoryStatDao, VmTlabStatDAO vmTlabStatDao, Clock clock, String vmId) {
        this.memDAO = vmMemoryStatDao;
        this.tlabDAO = vmTlabStatDao;
        this.clock = clock;
        this.vmId = vmId;
        this.writerId = writerId;
    }

    @Override
    public void countersUpdated(VmUpdate update) {
        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(update);
        recordMemoryStat(extractor);
        recordTlabStat(extractor);
    }

    void recordMemoryStat(VmMemoryDataExtractor extractor) {
        try {
            long timestamp = clock.getRealTimeMillis();

            long metaspaceMaxCapacity = extractor.getMetaspaceMaxCapacity(VmMemoryStat.UNKNOWN);
            long metaspaceMinCapacity = extractor.getMetaspaceMinCapacity(VmMemoryStat.UNKNOWN);
            long metaspaceCapacity = extractor.getMetaspaceCapacity(VmMemoryStat.UNKNOWN);
            long metaspaceUsed = extractor.getMetaspaceUsed(VmMemoryStat.UNKNOWN);

            Long maxGenerations = extractor.getTotalGcGenerations();
            if (maxGenerations != null) {
                List<Generation> generations = new ArrayList<Generation>(maxGenerations.intValue());
                for (int generation = 0; generation < maxGenerations; generation++) {
                    Generation g = createGeneration(extractor, generation);
                    if (g != null) {
                        Long maxSpaces = extractor.getTotalSpaces(generation);
                        if (maxSpaces != null) {
                            List<Space> spaces = new ArrayList<Space>(maxSpaces.intValue());
                            for (int space = 0; space < maxSpaces; space++) {
                                Space s = createSpace(extractor, generation,
                                        space);
                                if (s != null) {
                                    spaces.add(s);
                                }
                            }
                            g.setSpaces(spaces.toArray(new Space[spaces.size()]));
                            generations.add(g);
                        }
                        else {
                            logWarningOnce("Unable to determine number of spaces in generation "
                                    + generation + " for VM " + vmId + ". Skipping generation");
                        }
                    }
                }
                VmMemoryStat stat = new VmMemoryStat(writerId, timestamp, vmId, 
                        generations.toArray(new Generation[generations.size()]),
                        metaspaceMaxCapacity, metaspaceMinCapacity, metaspaceCapacity, metaspaceUsed);
                memDAO.putVmMemoryStat(stat);
            }
            else {
                logWarningOnce("Unable to determine number of generations for VM " + vmId);
            }
        } catch (VmUpdateException e) {
            logger.log(Level.WARNING, "Error gathering memory info for VM " + vmId, e);
        }
    }

    private Generation createGeneration(VmMemoryDataExtractor extractor,
            int generation) throws VmUpdateException {
        String name = extractor.getGenerationName(generation);
        if (name == null) {
            logWarningOnce("Unable to determine name of generation " 
                    + generation + " for VM " + vmId);
            return null;
        }
        Long capacity = extractor.getGenerationCapacity(generation);
        if (capacity == null) {
            logWarningOnce("Unable to determine capacity of generation " 
                    + generation + " for VM " + vmId);
            return null;
        }
        Long maxCapacity = extractor.getGenerationMaxCapacity(generation);
        if (maxCapacity == null) {
            logWarningOnce("Unable to determine max capacity of generation " 
                    + generation + " for VM " + vmId);
            return null;
        }
        String collector = extractor.getGenerationCollector(generation);
        if (collector == null) {
            logWarningOnce("Unable to determine collector of generation " 
                    + generation + " for VM " + vmId);
            return null;
        }
        
        Generation g = new Generation();
        g.setName(name);
        g.setCapacity(capacity);
        g.setMaxCapacity(maxCapacity);
        g.setCollector(collector);
        return g;
    }

    private Space createSpace(VmMemoryDataExtractor extractor, int generation,
            int space) throws VmUpdateException {
        String name = extractor.getSpaceName(generation, space);
        if (name == null) {
            logWarningOnce("Unable to determine name of space " + space 
                    + " in generation " + generation + " for VM " + vmId);
            return null;
        }
        Long capacity = extractor.getSpaceCapacity(generation, space);
        if (capacity == null) {
            logWarningOnce("Unable to determine capacity of space " + space 
                    + " in generation " + generation + " for VM " + vmId);
            return null;
        }
        Long maxCapacity = extractor.getSpaceMaxCapacity(generation, space);
        if (maxCapacity == null) {
            logWarningOnce("Unable to determine max capacity of space " + space 
                    + " in generation " + generation + " for VM " + vmId);
            return null;
        }
        Long used = extractor.getSpaceUsed(generation, space);
        if (used == null) {
            logWarningOnce("Unable to determine used memory of space " + space 
                    + " in generation " + generation + " for VM " + vmId);
            return null;
        }
        
        Space s = new Space();
        s.setIndex(space);
        s.setName(name);
        s.setCapacity(capacity);
        s.setMaxCapacity(maxCapacity);
        s.setUsed(used);
        return s;
    }

    void recordTlabStat(VmMemoryDataExtractor extractor) {
        long timestamp = clock.getRealTimeMillis();

        long allocatingThreads = extractor.getTlabTotalAllocatingThreads(VmTlabStat.UNKNOWN);
        long totalAllocations = extractor.getTlabTotalAllocations(VmTlabStat.UNKNOWN);
        long refills = extractor.getTlabTotalRefills(VmTlabStat.UNKNOWN);
        long maxRefills = extractor.getTlabMaxRefills(VmTlabStat.UNKNOWN);
        long slowAllocs = extractor.getTlabTotalSlowAllocs(VmTlabStat.UNKNOWN);
        long maxSlowAllocs = extractor.getTlabMaxSlowAllocs(VmTlabStat.UNKNOWN);
        long gcWaste = extractor.getTlabTotalGcWaste(VmTlabStat.UNKNOWN);
        long maxGcWaste = extractor.getTlabMaxGcWaste(VmTlabStat.UNKNOWN);
        long slowWaste = extractor.getTlabTotalSlowWaste(VmTlabStat.UNKNOWN);
        long maxSlowWaste = extractor.getTlabMaxSlowWaste(VmTlabStat.UNKNOWN);
        long fastWaste = extractor.getTlabTotalFastWaste(VmTlabStat.UNKNOWN);
        long maxFastWaste = extractor.getTlabMaxFastWaste(VmTlabStat.UNKNOWN);

        VmTlabStat stat = new VmTlabStat(timestamp, writerId, vmId,
                allocatingThreads, totalAllocations,
                refills, maxRefills,
                slowAllocs, maxSlowAllocs,
                gcWaste, maxGcWaste,
                slowWaste, maxSlowWaste,
                fastWaste, maxFastWaste);

        tlabDAO.putStat(stat);
    }

    private void logWarningOnce(String message) {
        if (!error) {
            logger.log(Level.WARNING, message);
            logger.log(Level.WARNING, "Further warnings will be ignored");
            error = true;
        }
    }

}

