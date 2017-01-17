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

package com.redhat.thermostat.storage.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;

@Entity
public class BackendInformation extends BasePojo implements Ordered {

    private String name;
    private String description;
    private boolean isActive;
    private boolean observeNewJvm;
    private int[] pids;
    private int orderValue;
    private Map<String, String> configuration = new HashMap<String,String>();

    public BackendInformation() {
        this(null);
    }
    
    public BackendInformation(String writerId) {
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

    @Persist
    public String getDescription() {
        return description;
    }

    @Persist
    public void setDescription(String description) {
        this.description = description;
    }

    @Persist
    public boolean isObserveNewJvm() {
        return observeNewJvm;
    }

    @Persist
    public void setObserveNewJvm(boolean observeNewJvm) {
        this.observeNewJvm = observeNewJvm;
    }

    @Persist
    public int[] getPids() {
        return pids;
    }

    @Persist
    public void setPids(int[] pids) {
        this.pids = pids;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    @Persist
    public boolean isActive() {
        return isActive;
    }

    @Persist
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    @Override
    @Persist
    public int getOrderValue() {
        return orderValue;
    }
    
    @Persist
    public void setOrderValue(int orderValue) {
        this.orderValue = orderValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BackendInformation)) {
            return false;
        }
        BackendInformation other = (BackendInformation) obj;
        return Objects.equals(this.name, other.name) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.configuration, other.configuration) &&
                Objects.equals(this.isActive, other.isActive) &&
                Objects.equals(this.observeNewJvm, other.observeNewJvm) &&
                Arrays.equals(this.pids, other.pids) &&
                Objects.equals(this.orderValue, other.orderValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, configuration, isActive, observeNewJvm, pids, orderValue);
    }

}

