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

package com.redhat.thermostat.thread.model;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;

/**
 * Represents a single delta variation of a Thread state.
 */
@Entity
public class ThreadState extends ThreadPojo {

    private long probeStartTime;
    private long probeEndTime;
    
    private String state;

    public ThreadState() {
        this(null, null);
    }
    
    public ThreadState(String wID, ThreadHeader header) {
        super(wID, header);
    }

    @Persist
    public void setProbeEndTime(long probeEndTime) {
        this.probeEndTime = probeEndTime;
    }

    @Persist
    public void setProbeStartTime(long probeStartTime) {
        this.probeStartTime = probeStartTime;
    }
    
    @Persist
    public long getProbeEndTime() {
        return probeEndTime;
    }
    
    @Persist
    public long getProbeStartTime() {
        return probeStartTime;
    }
    
    public Range<Long> getRange() {
        return new Range<Long>(probeStartTime, probeEndTime);
    }
    
    @Persist
    public String getState() {
        return state;
    }
    
    @Persist
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        ThreadHeader header = getHeader();
        String name = null;
        if (header != null) {
            name = header.getThreadName();
        }
        return "ThreadState: [name: " + name + "state: " + getState() + ", range: " + getRange() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreadState that = (ThreadState) o;

        if (probeEndTime != that.probeEndTime) return false;
        if (probeStartTime != that.probeStartTime) return false;

        if (!super.equals(o)) return false;

        if (state != that.state) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (probeStartTime ^ (probeStartTime >>> 32));
        result = 31 * result + (int) (probeEndTime ^ (probeEndTime >>> 32));
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
    }
}
