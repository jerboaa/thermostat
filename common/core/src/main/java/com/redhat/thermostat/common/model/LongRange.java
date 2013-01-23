/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common.model;


/**
 * A class representing a span of units from min to max. <br /><br />
 * 
 * The units and the meaning of the range are left to the user, including the
 * fact that the range are inclusive or exclusive of the extremes.
 */
public class LongRange {

    long min;
    long max;
    
    /**
     * Creates a new LongRange with a span of 0 units.
     */
    public LongRange() {
    }
    
    /**
     * Creates a new Range that span from min to max.
     */
    public LongRange(long min, long max) {
        this.min = min;
        this.max = max;
    }
    
    public void setMax(long max) {
        this.max = max;
    }
    
    public long getMax() {
        return max;
    }
    
    public void setMin(long min) {
        this.min = min;
    }
    
    public long getMin() {
        return min;
    }
    
    @Override
    public String toString() {
        return "[" + min + " ->" + max +  "("+ (max - min) + ")]";
    }
}

