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

package com.redhat.thermostat.thread.collector.impl;

import java.lang.Thread.State;
import java.util.Arrays;

import com.redhat.thermostat.thread.collector.ThreadInfo;

public class ThreadMXInfo implements ThreadInfo {

    private StackTraceElement[] stackTrace;
    private long threadID;
    private State threadState;
    private String name;
    private long allocatedBytes;
    
    private long threadCpuTime;
    private long threadUserTime;
    private long blockedCount;
    private long waitedCount;
    
    private long timestamp;
    
    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    @Override
    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
    
    @Override
    public String toString() {
        return "ThreadMXInfo [name=" + name
                + ", threadID=" + threadID + ", threadState=" + threadState
                + ", stackTrace=" + Arrays.toString(stackTrace)
                + ", allocatedBytes=" + allocatedBytes
                + ", threadCpuTime=" + threadCpuTime + ", threadUserTime="
                + threadUserTime + ", blockedCount=" + blockedCount
                + ", waitedCount=" + waitedCount + ", timestamp=" + timestamp
                + "]";
    }

    public void setName(String threadName) {
        this.name = threadName;
    }

    public void setID(long threadID) {
        this.threadID = threadID;
    }

    public void setState(State threadState) {
        this.threadState = threadState;
    }

    public void setAllocatedBytes(long allocatedBytes) {
        this.allocatedBytes = allocatedBytes;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public long getAllocatedBytes() {
        return allocatedBytes;
    }
    
    @Override
    public long getThreadID() {
        return threadID;
    }
    
    @Override
    public State getState() {
        return threadState;
    }
    
    @Override
    public long getTimeStamp() {
        return timestamp;
    }
    
    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setCPUTime(long threadCpuTime) {
        this.threadCpuTime = threadCpuTime;
    }

    public void setUserTime(long threadUserTime) {
       this.threadUserTime = threadUserTime;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }
    
    @Override
    public long getBlockedCount() {
        return blockedCount;
    }
    
    @Override
    public long getWaitedCount() {
        return waitedCount;
    }
    
    @Override
    public long getCpuTime() {
        return threadCpuTime;
    }
    
    @Override
    public long getUserTime() {
        return threadUserTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (threadID ^ (threadID >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ThreadMXInfo other = (ThreadMXInfo) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (threadID != other.threadID)
            return false;
        return true;
    }
}
