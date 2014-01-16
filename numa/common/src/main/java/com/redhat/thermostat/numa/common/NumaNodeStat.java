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

package com.redhat.thermostat.numa.common;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * Statistics about a NUMA node.
 *
 * The <code>nodeId</code> is an integer that identifies a node. The other properties
 * correspond to properties of the same name from
 *
 * <code>/sys/devices/system/node/node${nodeId}/numastat</code>
 *
 * or <code>man 8 numastat</code>.
 */
@Entity
public class NumaNodeStat implements Pojo {

    private long numaHit = -1;
    private long numaMiss = -1;
    private long numaForeign = -1;
    private long interleaveHit = -1;
    private long localNode = -1;
    private long otherNode = -1;
    private int nodeId = -1;

    @Persist
    public long getNumaHit() {
        return numaHit;
    }

    @Persist
    public void setNumaHit(long numaHit) {
        this.numaHit = numaHit;
    }

    @Persist
    public long getNumaMiss() {
        return numaMiss;
    }

    @Persist
    public void setNumaMiss(long numaMiss) {
        this.numaMiss = numaMiss;
    }

    @Persist
    public long getNumaForeign() {
        return numaForeign;
    }

    @Persist
    public void setNumaForeign(long numaForeign) {
        this.numaForeign = numaForeign;
    }

    @Persist
    public long getInterleaveHit() {
        return interleaveHit;
    }

    @Persist
    public void setInterleaveHit(long interleaveHit) {
        this.interleaveHit = interleaveHit;
    }

    @Persist
    public long getLocalNode() {
        return localNode;
    }

    @Persist
    public void setLocalNode(long localNode) {
        this.localNode = localNode;
    }

    @Persist
    public long getOtherNode() {
        return otherNode;
    }

    @Persist
    public void setOtherNode(long otherNode) {
        this.otherNode = otherNode;
    }

    @Persist
    public int getNodeId() {
        return nodeId;
    }

    @Persist
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String toString() {
        return "NumaStat: nodeId: " + nodeId + ", numaHit: " + numaHit + ", numaMiss: " + numaMiss + ", numaForeign: " + numaForeign + ", interleaveHit: " + interleaveHit + ", localNode: " + localNode + ", otherNode: " + otherNode;
    }
}

