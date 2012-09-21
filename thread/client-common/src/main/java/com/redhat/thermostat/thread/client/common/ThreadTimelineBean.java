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

public class ThreadTimelineBean implements Cloneable {

    private long startTime;
    private long stopTime;
    private String name;
    private Thread.State state;
    
    private boolean highlight;
    
    public boolean isHighlight() {
        return highlight;
    }
    
    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }
    
    public Thread.State getState() {
        return state;
    }
    
    public void setState(Thread.State state) {
        this.state = state;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getStopTime() {
        return stopTime;
    }
    
    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    @Override
    public String toString() {
        return "ThreadTimelineBean [name=" + name + ", state=" + state
                + ", startTime=" + startTime + " (" + new Date(startTime) + ")"
                + ", stopTime=" + stopTime + " (" + new Date(stopTime) + ")]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (startTime ^ (startTime >>> 32));
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + (int) (stopTime ^ (stopTime >>> 32));
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
        ThreadTimelineBean other = (ThreadTimelineBean) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (startTime != other.startTime)
            return false;
        if (state != other.state)
            return false;
        if (stopTime != other.stopTime)
            return false;
        return true;
    }
    
    /**
     * NOTE: A {@link ThreadTimelineBean} is contains another if they are
     * either equals, or the the name, state and start time are the same
     * (in other words, this method does not check the stop time, and is not a
     * strict Set operation).
     */
    public boolean contains(ThreadTimelineBean other) {
        if (equals(other)) {
            return true;
        }
        if (getName().equals(other.getName())      &&
            getState().equals(other.getState())    &&
            getStartTime() == other.getStartTime())
        {
            return true;
        }
        
        return false;
    }
    
    @Override
    public ThreadTimelineBean clone() {
        ThreadTimelineBean copy = new ThreadTimelineBean();
        copy.name = this.name;
        copy.startTime = this.startTime;
        copy.stopTime = this.stopTime;
        copy.state = this.state;
        return copy;
    }
}
