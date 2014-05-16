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

package com.redhat.thermostat.thread.harvester;

import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class VMCapsHelper {

    private final ThreadDao threadDao;
    private final SystemClock systemClock;
    private final String vmId;
    private final WriterID writerId;

    public VMCapsHelper(ThreadDao threadDao, SystemClock systemClock, String vmId, WriterID writerId) {

        this.threadDao = threadDao;
        this.systemClock = systemClock;
        this.vmId = vmId;
        this.writerId = writerId;
    }

    public void saveVMCapabilities(ThreadMXBean collectorBean) {

        VMThreadCapabilities caps = new VMThreadCapabilities(writerId.getWriterID());

        List<String> features = new ArrayList<>();
        if (collectorBean.isThreadCpuTimeSupported()) {
            features.add(ThreadDao.CPU_TIME);
        }

        if (collectorBean.isThreadContentionMonitoringSupported()) {
            features.add(ThreadDao.CONTENTION_MONITOR);
        }

        if (collectorBean instanceof com.sun.management.ThreadMXBean) {
            com.sun.management.ThreadMXBean sunBean =
                (com.sun.management.ThreadMXBean) collectorBean;
            try {
                if (sunBean.isThreadAllocatedMemorySupported()) {
                    features.add(ThreadDao.THREAD_ALLOCATED_MEMORY);
                }
            } catch (Exception ignore) {}
         }

        caps.setSupportedFeaturesList(features.toArray(new String[features.size()]));
        caps.setVmId(vmId);

        threadDao.saveCapabilities(caps);
    }
}
