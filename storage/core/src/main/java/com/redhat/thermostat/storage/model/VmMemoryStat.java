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

package com.redhat.thermostat.storage.model;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;

@Entity
public class VmMemoryStat extends BasePojo implements TimeStampedPojo {

    @Entity
    public static class Generation implements Pojo {
        public static final String COLLECTOR_NONE = "none";
        private String name;
        private long capacity;
        private long maxCapacity;
        private Space[] spaces;
        private String collector;

        @Persist
        public String getName() {
            return name;
        }

        @Persist
        public void setName(String name) {
            this.name = name;
        }

        @Persist
        public long getCapacity() {
            return capacity;
        }

        @Persist
        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        @Persist
        public long getMaxCapacity() {
            return maxCapacity;
        }

        @Persist
        public void setMaxCapacity(long maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

        @Persist
        public Space[] getSpaces() {
            return spaces;
        }

        @Persist
        public void setSpaces(Space[] spaces) {
            this.spaces = spaces;
        }

        @Persist
        public String getCollector() {
            return collector;
        }

        @Persist
        public void setCollector(String collector) {
            this.collector = collector;
        }

        public Space getSpace(String string) {
            for (Space s : spaces) {
                if (s.name.equals(string)) {
                    return s;
                }
            }
            return null;
        }
    }

    @Entity
    public static class Space implements Pojo {

        private int index;
        private String name;
        private long capacity;
        private long maxCapacity;
        private long used;

        @Persist
        public int getIndex() {
            return index;
        }

        @Persist
        public void setIndex(int index) {
            this.index = index;
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
        public long getCapacity() {
            return capacity;
        }

        @Persist
        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        @Persist
        public long getMaxCapacity() {
            return maxCapacity;
        }

        @Persist
        public void setMaxCapacity(long maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

        @Persist
        public long getUsed() {
            return used;
        }

        @Persist
        public void setUsed(long used) {
            this.used = used;
        }

    }

    private Generation[] generations;
    private long timestamp;
    private int vmId;

    public VmMemoryStat() {
        this(-1, -1, null);
    }

    public VmMemoryStat(long timestamp, int vmId, Generation[] generations) {
        this.timestamp = timestamp;
        this.vmId = vmId;
        if (generations != null) {
            this.generations = generations;
        }
    }

    @Persist
    public int getVmId() {
        return vmId;
    }

    @Persist
    public void setVmId(int vmId) {
        this.vmId = vmId;
    }

    @Persist
    @Override
    public long getTimeStamp() {
        return timestamp;
    }

    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timestamp = timeStamp;
    }

    @Persist
    public Generation[] getGenerations() {
        return generations;
    }

    @Persist
    public void setGenerations(Generation[] generations) {
        this.generations = generations;
    }

    public Generation getGeneration(String name) {
        for (Generation g : generations) {
            if (g.name.equals(name)) {
                return g;
            }
        }
        return null;
    }

}
