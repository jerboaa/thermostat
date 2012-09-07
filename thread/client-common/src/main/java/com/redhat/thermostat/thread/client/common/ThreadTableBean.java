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

package com.redhat.thermostat.thread.client.common;

import java.util.Date;
import java.util.Objects;

public class ThreadTableBean {

    private String name;
    private long id;
    private long stop;
    private long start;
    private long waitedCount;
    private long blockedCount;

    private double runningPercent;
    private double waitingPercent;
    private double monitorPercent;
    private double sleepingPercent;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, Long.valueOf(this.id));
    }
    
    @Override
    public String toString() {
        return "ThreadTableBean [name=" + name + ", id=" + id + ", start time="
                + start + " (" + new Date(start) + "), stop time="
                + stop +  " (" + new Date(stop) + ")]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ThreadTableBean other = (ThreadTableBean) obj;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public void setStartTimeStamp(long start) {
        this.start = start;
    }
    
    public void setStopTimeStamp(long stop) {
        this.stop = stop;
    }
    
    public long getStartTimeStamp() {
        return start;
    }
    
    public long getStopTimeStamp() {
        return stop;
    }

    public long getBlockedCount() {
        return blockedCount;
    }
    
    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }
    
    public long getWaitedCount() {
        return waitedCount;
    }
    
    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }
    
    public double getWaitingPercent() {
        return waitingPercent;
    }
    
    public void setWaitingPercent(double waitingPercent) {
        this.waitingPercent = waitingPercent;
    }
    
    public double getRunningPercent() {
        return runningPercent;
    }
   
    public void setRunningPercent(double runningPercent) {
        this.runningPercent = runningPercent;
    }
    
    public void setMonitorPercent(double monitorPercent) {
        this.monitorPercent = monitorPercent;
    }
    
    public double getMonitorPercent() {
        return monitorPercent;
    }
    
    public void setSleepingPercent(double sleepingPercent) {
        this.sleepingPercent = sleepingPercent;
    }
    
    public double getSleepingPercent() {
        return sleepingPercent;
    }
}
