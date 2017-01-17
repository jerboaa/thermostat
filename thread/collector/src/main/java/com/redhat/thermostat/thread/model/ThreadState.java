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

package com.redhat.thermostat.thread.model;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.experimental.statement.Category;
import com.redhat.thermostat.storage.core.experimental.statement.Indexed;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.thread.dao.internal.ThreadDaoCategories;

/**
 * Represents a single delta variation of a Thread state.
 */
@Category(ThreadDaoCategories.Categories.STATE)
@Entity
public class ThreadState extends BasePojo implements TimeStampedPojo {

    private long timeStamp;
    private String vmId;
    private String name;
    private String state;
    private String session;
    private long id;
    private boolean suspended;
    private boolean inNative;
    private long blockedCount;
    private long blockedTime;
    private long waitedCount;
    private long waitedTime;
    private String stackTrace;

    public ThreadState() {
        this(null);
    }
    
    public ThreadState(String writerId) {
        super(writerId);
    }

    @Persist
    public String getName() {
        return name;
    }

    @Persist
    public void setName(String name) {
        this.name = name;
    }

    @Indexed
    @Persist
    public String getVmId() {
        return vmId;
    }

    @Indexed
    @Persist
    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    @Indexed
    @Persist
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Indexed
    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
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
        return "ThreadState: [name: " + name + " state: " + getState() +
               ", timestamp: " + timeStamp + "]";
    }

    @Persist
    public void setSession(String session) {
        this.session = session;
    }

    @Persist
    public String getSession() {
        return session;
    }

    @Persist
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    @Persist
    public void setId(long id) {
        this.id = id;
    }

    @Persist
    public long getId() {
        return id;
    }

    @Persist
    public boolean isSuspended() {
        return suspended;
    }

    @Persist
    public void setInNative(boolean inNative) {
        this.inNative = inNative;
    }

    @Persist
    public boolean isInNative() {
        return inNative;
    }

    @Persist
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Persist
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ThreadState that = (ThreadState) o;

        if (id != that.id) return false;
        if (inNative != that.inNative) return false;
        if (suspended != that.suspended) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (session != null ? !session.equals(that.session) : that.session != null)
            return false;
        if (state != null ? !state.equals(that.state) : that.state != null)
            return false;
        if (vmId != null ? !vmId.equals(that.vmId) : that.vmId != null)
            return false;
        if (stackTrace != null ? !stackTrace.equals(that.stackTrace) : that.stackTrace != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (vmId != null ? vmId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (session != null ? session.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (suspended ? 1 : 0);
        result = 31 * result + (inNative ? 1 : 0);
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        return result;
    }

    @Persist
    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    @Persist
    public long getBlockedCount() {
        return blockedCount;
    }

    @Persist
    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }

    @Persist
    public long getBlockedTime() {
        return blockedTime;
    }

    @Persist
    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }

    @Persist
    public long getWaitedCount() {
        return waitedCount;
    }

    @Persist
    public void setWaitedTime(long waitedTime) {
        this.waitedTime = waitedTime;
    }

    @Persist
    public long getWaitedTime() {
        return waitedTime;
    }
}
