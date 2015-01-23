/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.HostPortsParser;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;

@Entity
public class AgentInformation extends BasePojo {

    private long startTime;
    private long stopTime;

    private boolean alive;
    private String address;

    public AgentInformation() {
        this(null);
    }
    
    public AgentInformation(String writerId) {
        super(writerId);
    }
    
    @Persist
    public long getStartTime() {
        return startTime;
    }

    @Persist
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Persist
    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }
    
    @Persist
    public long getStopTime() {
        return stopTime;
    }

    @Persist
    public boolean isAlive() {
        return alive;
    }
    
    @Persist
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Persist
    public String getConfigListenAddress() {
        return address;
    }

    @Persist
    public void setConfigListenAddress(String address) {
        this.address = address;
    }

    public InetSocketAddress getRequestQueueAddress() {
        String address = getConfigListenAddress();
        HostPortsParser parser = new HostPortsParser(address);
        parser.parse();
        List<HostPortPair> result = parser.getHostsPorts();
        HostPortPair hostAndPort = result.get(0);
        InetSocketAddress target = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
        return target;
    }

    @Override
    public String toString() {
        return "agent " + getAgentId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AgentInformation)) {
            return false;
        }
        AgentInformation other = (AgentInformation) obj;
        return super.equals(other) &&
                Objects.equals(this.alive, other.alive) &&
                Objects.equals(this.address, other.address) &&
                Objects.equals(this.startTime, other.startTime) &&
                Objects.equals(this.stopTime, other.stopTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAgentId(), alive, address, startTime, stopTime);
    }
}

