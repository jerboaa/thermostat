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

import java.util.Objects;
import java.util.UUID;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

/**
 * The common {@link Entity} for Thread related informations. 
 */
@Entity
public class ThreadHeader extends BasePojo implements TimeStampedPojo {

    private long timestamp;
    private String vmId;
    private String name;
    private long threadID;

    private String referenceID;

    public ThreadHeader() {
        this(null);
    }

    public ThreadHeader(String writerId) {
        this(writerId, UUID.randomUUID().toString());
    }

    public ThreadHeader(String writerId, String referenceID) {
        super(writerId);
        this.referenceID = referenceID;
    }

    @Persist
    public String getReferenceID() {
        return referenceID;
    }

    @Persist
    public void setReferenceID(String referenceID) {
        this.referenceID = referenceID;
    }

    @Persist
    public void setThreadName(String threadName) {
        this.name = threadName;
    }

    @Persist
    public void setThreadId(long threadID) {
        this.threadID = threadID;
    }

    @Persist
    public String getThreadName() {
        return name;
    }
    
    @Persist
    public long getThreadId() {
        return threadID;
    }
    
    @Persist
    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    @Persist
    public String getVmId() {
        return vmId;
    }
    
    @Override
    @Persist
    public long getTimeStamp() {
        return timestamp;
    }
    
    @Persist
    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (threadID ^ (threadID >>> 32));
        result = prime * result + ((vmId == null) ? 0 : vmId.hashCode());
        
        String agentID = getAgentId();
        result = prime * result + ((agentID == null) ? 0 : agentID.hashCode());
        
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ThreadHeader other = (ThreadHeader) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (threadID != other.threadID) {
            return false;
        }
        if (vmId == null) {
            if (other.vmId != null) {
                return false;
            }
        } else if (!vmId.equals(other.vmId)) {
            return false;
        }
        
        String agentID = getAgentId();
        String otherAgentID = getAgentId();
        return Objects.equals(agentID, otherAgentID);
    }

    @Override
    public String toString() {
        return "ThreadHeader [name=" + name + ", threadID=" + threadID
                + ", vmId=" + vmId + ", timestamp=" + timestamp + "]";
    }
}
