/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.client.core;

import com.redhat.thermostat.common.Size;

public class Payload implements Cloneable {

    private String name;
    private String tooltip;
    
    private double capacity;
    private double maxCapacity;
    private double maxUsed;
    private double used;
    
    private Size.Unit usedUnit;
    private Size.Unit capacityUnit;
    
    private StatsModel model;
    
    public void setModel(StatsModel model) {
        this.model = model;
    }
    
    public StatsModel getModel() {
        return model;
    }
    
    public void setCapacityUnit(Size.Unit capacityUnit) {
        this.capacityUnit = capacityUnit;
    }
    
    public Size.Unit getCapacityUnit() {
        return capacityUnit;
    }
    
    public void setUsedUnit(Size.Unit usedUnit) {
        this.usedUnit = usedUnit;
    }
    
    public Size.Unit getUsedUnit() {
        return usedUnit;
    }
    
    public double getMaxCapacity() {
        return maxCapacity;
    }
    
    public void setMaxCapacity(double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    
    public double getUsed() {
        return used;
    }
    
    public void setUsed(double used) {
        this.used = used;
    }
    
    public double getMaxUsed() {
        return maxUsed;
    }
    
    public void setMaxUsed(double maxUsed) {
        this.maxUsed = maxUsed;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTooltip() {
        return tooltip;
    }
    
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
    
    @Override
    public Payload clone() {
        
        Payload copy = new Payload();
        
        copy.used = used;
        copy.capacity = capacity;
        copy.capacityUnit = capacityUnit;
        copy.maxCapacity = maxCapacity;
        copy.maxUsed = maxUsed;
        copy.model = model.clone();
        copy.name = name;
        copy.tooltip = tooltip;
        copy.usedUnit = usedUnit;
        
        return copy;
    }
}

