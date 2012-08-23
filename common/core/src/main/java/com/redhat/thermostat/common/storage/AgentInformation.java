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

package com.redhat.thermostat.common.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgentInformation {

    public static final Category AGENT_INFO_CATEGORY =
            new Category(StorageConstants.CATEGORY_AGENT_CONFIG, Key.AGENT_ID);

    public static final Key<Boolean> AGENT_ALIVE_KEY = new Key<>("alive", false);
    
    private long startTime;
    private long stopTime;

    private boolean alive;
    private String address;

    private List<BackendInformation> backends = new ArrayList<BackendInformation>();
    
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }
    
    public long getStopTime() {
        return stopTime;
    }
    
    public List<BackendInformation> getBackends() {
        return Collections.unmodifiableList(backends);
    }

    public boolean isAlive() {
        return alive;
    }
    
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
    
    public void addBackend(BackendInformation backend) {
        backends.add(backend);
    }

    public String getConfigListenAddress() {
        return address;
    }

    public void setConfigListenAddress(String address) {
        this.address = address;
    }
}
