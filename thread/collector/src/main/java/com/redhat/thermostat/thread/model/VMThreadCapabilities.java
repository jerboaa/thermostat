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

package com.redhat.thermostat.thread.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Entity;
import com.redhat.thermostat.common.storage.Persist;
import com.redhat.thermostat.thread.dao.ThreadDao;

@Entity
public class VMThreadCapabilities implements Pojo {
    
    private Set<String> features = new HashSet<>();

    private int vmId;

    @Persist
    public void setVmId(int vmId) {
        this.vmId = vmId;
    }

    @Persist
    public int getVmId() {
        return vmId;
    }

    public boolean supportCPUTime() {
        return features.contains(ThreadDao.CPU_TIME);
    }

    public boolean supportContentionMonitor() {
        return features.contains(ThreadDao.CONTENTION_MONITOR);
    }

    public String toString() {
        return "[supportCPU: " + supportCPUTime() + ", supportContention: " + supportContentionMonitor() +
               ", supportThreadAllocatedMemory: " + supportThreadAllocatedMemory() + "]";
    }

    public boolean supportThreadAllocatedMemory() {
        return features.contains(ThreadDao.THREAD_ALLOCATED_MEMORY);
    }

    @Persist
    public List<String> getSupportedFeaturesList() {
        return new ArrayList<>(features);
    }

    @Persist
    public void setSupportedFeaturesList(List<String> featuresList) {
        features = new HashSet<>(featuresList);
    }

    public void addFeature(String feature) {
        features.add(feature);
    }
}