/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.agent.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;

public class VmGcVmListener implements VmUpdateListener {
    
    private static final Logger logger = LoggingUtils.getLogger(VmGcVmListener.class);

    private final String vmId;
    private final VmGcStatDAO gcDAO;
    private final String writerId;
    
    private boolean error;

    public VmGcVmListener(String writerId, VmGcStatDAO vmGcStatDao, String vmId) {
        gcDAO = vmGcStatDao;
        this.vmId = vmId;
        this.writerId = writerId;
    }

    @Override
    public void countersUpdated(VmUpdate update) {
        VmGcDataExtractor extractor = new VmGcDataExtractor(update);
        recordGcStat(extractor);
    }

    void recordGcStat(VmGcDataExtractor extractor) {
        try {
            Long collectors = extractor.getTotalCollectors();
            if (collectors != null) {
                for (int i = 0; i < collectors; i++) {
                    long timestamp = System.currentTimeMillis();
                    String name = extractor.getCollectorName(i);
                    if (name != null) {
                        Long invocations = extractor.getCollectorInvocations(i);
                        if (invocations != null) {
                            Long time = extractor.getCollectorTime(i);
                            if (time != null) {
                                VmGcStat stat = new VmGcStat(writerId, vmId, timestamp,
                                        name, invocations, time);
                                gcDAO.putVmGcStat(stat);
                            }
                            else {
                                logWarningOnce("Unable to determine time spent by collector " 
                                        + i + " for VM " + vmId);
                            }
                        }
                        else {
                            logWarningOnce("Unable to determine number of invocations of collector " 
                                    + i + " for VM " + vmId);
                        }
                    }
                    else {
                        logWarningOnce("Unable to determine name of collector " + i + " for VM " + vmId);
                    }
                }
            }
            else {
                logWarningOnce("Unable to determine number of collectors for VM " + vmId);
            }
        } catch (VmUpdateException e) {
            logger.log(Level.WARNING, "Error gathering GC info for VM " + vmId, e);
        }
    }
    
    private void logWarningOnce(String message) {
        if (!error) {
            logger.log(Level.WARNING, message);
            logger.log(Level.WARNING, "Further warnings will be ignored");
            error = true;
        }
    }

}

