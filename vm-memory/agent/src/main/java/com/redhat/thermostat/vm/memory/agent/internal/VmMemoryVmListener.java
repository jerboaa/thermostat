/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class VmMemoryVmListener implements VmUpdateListener {
    
    private static final Logger logger = LoggingUtils.getLogger(VmMemoryVmListener.class);

    private final int vmId;
    private final VmMemoryStatDAO memDAO;

    public VmMemoryVmListener(VmMemoryStatDAO vmMemoryStatDao, int vmId) {
        memDAO = vmMemoryStatDao;
        this.vmId = vmId;
    }

    @Override
    public void countersUpdated(VmUpdate update) {
        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(update);
        recordMemoryStat(extractor);
    }

    void recordMemoryStat(VmMemoryDataExtractor extractor) {
        try {
            long timestamp = System.currentTimeMillis();
            int maxGenerations = (int) extractor.getTotalGcGenerations();
            Generation[] generations = new Generation[maxGenerations];
            for (int generation = 0; generation < maxGenerations; generation++) {
                Generation g = new Generation();
                g.setName(extractor.getGenerationName(generation));
                g.setCapacity(extractor.getGenerationCapacity(generation));
                g.setMaxCapacity(extractor.getGenerationMaxCapacity(generation));
                g.setCollector(extractor.getGenerationCollector(generation));
                generations[generation] = g;
                int maxSpaces = (int) extractor.getTotalSpaces(generation);
                Space[] spaces = new Space[maxSpaces];
                for (int space = 0; space < maxSpaces; space++) {
                    Space s = new Space();
                    s.setIndex((int) space);
                    s.setName(extractor.getSpaceName(generation, space));
                    s.setCapacity(extractor.getSpaceCapacity(generation, space));
                    s.setMaxCapacity(extractor.getSpaceMaxCapacity(generation, space));
                    s.setUsed(extractor.getSpaceUsed(generation, space));
                    spaces[space] = s;
                }
                g.setSpaces(spaces);
            }
            VmMemoryStat stat = new VmMemoryStat(timestamp, vmId, generations);
            memDAO.putVmMemoryStat(stat);
        } catch (VmUpdateException e) {
            logger.log(Level.WARNING, "error gathering memory info for vm " + vmId, e);
        }
    }


}

