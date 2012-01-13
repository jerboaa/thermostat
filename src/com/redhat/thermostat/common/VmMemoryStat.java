package com.redhat.thermostat.common;

import java.util.List;

public class VmMemoryStat {

    public static class Generation {
        public static final String COLLECTOR_NONE = "none";
        public String name;
        public long capacity;
        public long maxCapacity;
        public List<Space> spaces;
        public String collector;

        public Space getSpace(String string) {
            for (Space s : spaces) {
                if (s.name.equals(string)) {
                    return s;
                }
            }
            return null;
        }
    }

    public static class Space {
        public int index;
        public String name;
        public long capacity;
        public long maxCapacity;
        public long used;
    }

    private final List<Generation> generations;
    private final long timestamp;
    private final int vmId;

    public VmMemoryStat(long timestamp, int vmId, List<Generation> generations) {
        this.timestamp = timestamp;
        this.vmId = vmId;
        this.generations = generations;
    }

    public int getVmId() {
        return vmId;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public List<Generation> getGenerations() {
        return generations;
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
