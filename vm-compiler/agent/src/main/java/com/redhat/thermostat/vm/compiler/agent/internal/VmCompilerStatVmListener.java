/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.compiler.agent.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStat;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStatDao;

class VmCompilerStatVmListener implements VmUpdateListener {

    private static final Logger logger = LoggingUtils.getLogger(VmCompilerStatVmListener.class);

    private final VmCompilerStatDao dao;
    private final String vmId;
    private final String writerId;

    private boolean error;

    VmCompilerStatVmListener(String writerId, VmCompilerStatDao dao, String vmId) {
        this.dao = dao;
        this.vmId = vmId;
        this.writerId = writerId;
    }

    @Override
    public void countersUpdated(VmUpdate update) {
        try {
            long totalCompiles = longValueOrUnknown(update, "totalCompiles", vmId);
            long totalBailouts = longValueOrUnknown(update, "totalBailouts", vmId);
            long totalInvalidates = longValueOrUnknown(update, "totalInvalidates", vmId);
            long compilationTime = update.getPerformanceCounterLong("java.ci.totalTime") / update.getPerformanceCounterLong("sun.os.hrt.frequency");
            long lastSize = longValueOrUnknown(update, "lastSize", vmId);
            long lastType = longValueOrUnknown(update, "lastType", vmId);
            String lastMethod = update.getPerformanceCounterString("sun.ci.lastMethod");
            long lastFailedType = longValueOrUnknown(update, "lastFailedType", vmId);
            String lastFailedMethod = update.getPerformanceCounterString("sun.ci.lastFailedMethod");

            long timestamp = System.currentTimeMillis();
            VmCompilerStat stat = new VmCompilerStat(writerId, vmId, timestamp,
                    totalCompiles, totalBailouts, totalInvalidates,
                    compilationTime,
                    lastSize, lastType, lastMethod,
                    lastFailedType, lastFailedMethod);

            dao.putVmCompilerStat(stat);

        } catch (VmUpdateException e) {
            logger.log(Level.WARNING, "Error gathering class info for VM " + vmId, e);
        }
    }

    private long longValueOrUnknown(VmUpdate update, String name, String vmId) throws VmUpdateException {
        String fullName = "sun.ci." + name;
        Long value = update.getPerformanceCounterLong(fullName);
        if (value == null) {
            logWarningOnce("Unable to determine " + fullName + " for VM " + vmId);
        }
        return value == null ? VmCompilerStat.UNKNOWN : value;
    }

    private void logWarningOnce(String message) {
        if (!error) {
            logger.log(Level.WARNING, message);
            logger.log(Level.WARNING, "Further warnings will be ignored");
            error = true;
        }
    }

}
