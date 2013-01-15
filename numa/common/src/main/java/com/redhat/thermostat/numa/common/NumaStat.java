/*
 * Copyright 2013 Red Hat, Inc.
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

import com.redhat.thermostat.storage.model.BasePojo;

public class NumaStat extends BasePojo {

    private long numaHit = -1;
    private long numaMiss = -1;
    private long numaForeign = -1;
    private long interleaveHit = -1;
    private long localNode = -1;
    private long otherNode = -1;
    private int node = -1;

    public long getNumaHit() {
        return numaHit;
    }

    public void setNumaHit(long numaHit) {
        this.numaHit = numaHit;
    }

    public long getNumaMiss() {
        return numaMiss;
    }

    public void setNumaMiss(long numaMiss) {
        this.numaMiss = numaMiss;
    }

    public long getNumaForeign() {
        return numaForeign;
    }

    public void setNumaForeign(long numaForeign) {
        this.numaForeign = numaForeign;
    }

    public long getInterleaveHit() {
        return interleaveHit;
    }

    public void setInterleaveHit(long interleaveHit) {
        this.interleaveHit = interleaveHit;
    }

    public long getLocalNode() {
        return localNode;
    }

    public void setLocalNode(long localNode) {
        this.localNode = localNode;
    }

    public long getOtherNode() {
        return otherNode;
    }

    public void setOtherNode(long otherNode) {
        this.otherNode = otherNode;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public String toString() {
        return "NumaStat: node: " + node + ", numaHit: " + numaHit + ", numaMiss: " + numaMiss + ", numaForeign: " + numaForeign + ", interleaveHit: " + interleaveHit + ", localNode: " + localNode + ", otherNode: " + otherNode;
    }
}
