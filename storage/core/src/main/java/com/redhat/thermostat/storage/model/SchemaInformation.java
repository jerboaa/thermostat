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

package com.redhat.thermostat.storage.model;

import java.util.Objects;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;

@Entity
public class SchemaInformation implements TimeStampedPojo {

    private String categoryName;
    private long timeStamp;
    
    public SchemaInformation() {
    }
    
    @Persist
    public String getCategoryName() {
        return categoryName;
    }
    
    @Persist
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    @Persist
    public long getTimeStamp() {
        return this.timeStamp;
    }
    
    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    
    @Override
    public String toString() {
        return "name: " + getCategoryName() + "; timestamp: " + getTimeStamp();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SchemaInformation)) {
            return false;
        }
        
        SchemaInformation other = (SchemaInformation) obj;
        return Objects.equals(this.categoryName, other.categoryName) &&
               Objects.equals(this.timeStamp, other.timeStamp);    
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(categoryName, timeStamp);
    }
    
}