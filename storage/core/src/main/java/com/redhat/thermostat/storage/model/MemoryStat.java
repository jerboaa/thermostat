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

package com.redhat.thermostat.storage.model;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;

@Entity
public class MemoryStat extends BasePojo implements TimeStampedPojo {

    private long timeStamp;
    private long total;
    private long free;
    private long buffers;
    private long cached;
    private long swapTotal;
    private long swapFree;
    private long commitLimit;

    public MemoryStat() {
        super();
    }

    public MemoryStat(long timeStamp, long total, long free, long buffers, long cached, long swapTotal, long swapFree, long commitLimit) {
        this.timeStamp = timeStamp;
        this.total = total;
        this.free = free;
        this.buffers = buffers;
        this.cached = cached;
        this.swapTotal = swapTotal;
        this.swapFree = swapFree;
        this.commitLimit = commitLimit;
    }

    @Override
    @Persist
    public long getTimeStamp() {
        return timeStamp;
    }

    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Persist
    public long getTotal() {
        return total;
    }

    @Persist
    public void setTotal(long total) {
        this.total = total;
    }

    @Persist
    public long getFree() {
        return free;
    }

    @Persist
    public void setFree(long free) {
        this.free = free;
    }

    @Persist
    public long getBuffers() {
        return buffers;
    }

    @Persist
    public void setBuffers(long buffers) {
        this.buffers = buffers;
    }

    @Persist
    public long getCached() {
        return cached;
    }

    @Persist
    public void setCached(long cached) {
        this.cached = cached;
    }

    @Persist
    public long getSwapTotal() {
        return swapTotal;
    }

    @Persist
    public void setSwapTotal(long swapTotal) {
        this.swapTotal = swapTotal;
    }

    @Persist
    public long getSwapFree() {
        return swapFree;
    }

    @Persist
    public void setSwapFree(long swapFree) {
        this.swapFree = swapFree;
    }

    @Persist
    public long getCommitLimit() {
        return commitLimit;
    }

    @Persist
    public void setCommitLimit(long commitLimit) {
        this.commitLimit = commitLimit;
    }

}
