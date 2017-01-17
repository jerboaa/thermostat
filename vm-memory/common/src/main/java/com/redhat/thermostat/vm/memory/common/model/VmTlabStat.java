/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.common.model;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

@Entity
public class VmTlabStat extends BasePojo implements TimeStampedPojo {

    // See https://blogs.oracle.com/jonthecollector/entry/the_real_thing
    // See hotspot/src/share/vm/gc/shared/threadLocalAllocBuffer.hpp

    public static final long UNKNOWN = -1;

    private String vmId;
    private long timeStamp;

    private long allocThreads;
    private long totalAllocations;

    private long refills;
    private long maxRefills;

    private long slowAllocations;
    private long maxSlowAllocations;

    private long gcWaste;
    private long maxGcWaste;

    private long slowWaste;
    private long maxSlowWaste;

    private long fastWaste;
    private long maxFastWaste;

    /** for de-serialization only */
    public VmTlabStat() {
        super(null);
    }

    public VmTlabStat(long timeStamp, String agentId, String vmId,
            long threadCount, long totalAllocations,
            long refills, long maxRefills,
            long slowAllocations, long maxSlowAllocations,
            long gcWaste, long maxGcWaste,
            long slowWaste,    long maxSlowWaste,
            long fastWaste, long maxFastWaste) {
        super(agentId);
        this.vmId = vmId;
        this.timeStamp = timeStamp;
        this.allocThreads = threadCount;
        this.totalAllocations = totalAllocations;
        this.refills = refills;
        this.maxRefills = maxRefills;
        this.slowAllocations = slowAllocations;
        this.maxSlowAllocations = maxSlowAllocations;
        this.gcWaste = gcWaste;
        this.maxGcWaste = maxGcWaste;
        this.slowWaste = slowWaste;
        this.maxSlowWaste = maxSlowWaste;
        this.fastWaste = fastWaste;
        this.maxFastWaste = maxFastWaste;
    }

    @Persist
    public String getVmId() {
        return vmId;
    }

    @Persist
    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    @Persist
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /** Number of threads that did an allocation in the last epoch */
    @Persist
    public long getTotalAllocatingThreads() {
        return allocThreads;
    }

    @Persist
    public void setTotalAllocatingThreads(long allocThreads) {
        this.allocThreads = allocThreads;
    }

    /** Number of allocations in the last epoch */
    @Persist
    public long getTotalAllocations() {
        return totalAllocations;
    }

    @Persist
    public void setTotalAllocations(long allocations) {
        this.totalAllocations = allocations;
    }

    // Refills:

    @Persist
    public long getTotalRefills() {
        return refills;
    }

    @Persist
    public void setTotalRefills(long totalRefills) {
        this.refills = totalRefills;
    }

    /** The maximum refills by a single thread */
    @Persist
    public long getMaxRefills() {
        return maxRefills;
    }

    @Persist
    public void setMaxRefills(long maxRefills) {
        this.maxRefills = maxRefills;
    }

    // Slow Allocations:

    /** Total number of allocations done outside of a TLAB */
    @Persist
    public long getTotalSlowAllocations() {
        return slowAllocations;
    }

    @Persist
    public void setTotalSlowAllocations(long slowAllocations) {
        this.slowAllocations = slowAllocations;
    }

    /** Maximum number of allocations done outside of a TLAB by a single thread */
    @Persist
    public long getMaxSlowAllocations() {
        return maxSlowAllocations;
    }

    @Persist
    public void setMaxSlowAllocations(long maxSlowAllocations) {
        this.maxSlowAllocations = maxSlowAllocations;
    }

    // Refill Waste:

    /** Unused space in the current TLABs when GC starts */
    @Persist
    public long getTotalGcWaste() {
        return gcWaste;
    }

    @Persist
    public void setTotalGcWaste(long gcWaste) {
        this.gcWaste = gcWaste;
    }

    /** Max unused space for a single thread's TLAB when GC starts */
    @Persist
    public long getMaxGcWaste() {
        return maxGcWaste;
    }

    @Persist
    public void setMaxGcWaste(long maxGcWaste) {
        this.maxGcWaste = maxGcWaste;
    }

    /** Sum of unused space in TLABs when they're retired to allocate a new ones */
    @Persist
    public long getTotalSlowWaste() {
        return slowWaste;
    }

    @Persist
    public void setTotalSlowWaste(long slowWaste) {
        this.slowWaste = slowWaste;
    }

    /** Maximum unused space in TLABS when they are retired to allocate a new one */
    @Persist
    public long getMaxSlowWaste() {
        return maxSlowWaste;
    }

    @Persist
    public void setMaxSlowWaste(long maxSlowWaste) {
        this.maxSlowWaste = maxSlowWaste;
    }

    /** Sum of unused space in fast-allocated TLABs when they are retired to allocate a new one */
    @Persist
    public long getTotalFastWaste() {
        return fastWaste;
    }

    @Persist
    public void setTotalFastWaste(long fastWaste) {
        this.fastWaste = fastWaste;
    }

    /** Maximum unused space in a single thread's fast-allocated TLABs when they are retired to allocate a new one */
    @Persist
    public long getMaxFastWaste() {
        return maxFastWaste;
    }

    @Persist
    public void setMaxFastWaste(long maxFastWaste) {
        this.maxFastWaste = maxFastWaste;
    }

}
