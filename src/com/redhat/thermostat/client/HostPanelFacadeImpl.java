package com.redhat.thermostat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.NetworkInfo;
import com.redhat.thermostat.common.NetworkInterfaceInfo;

public class HostPanelFacadeImpl implements HostPanelFacade {

    private HostRef agent;
    private DB db;
    private DBCollection hostInfoCollection;
    private DBCollection networkInfoCollection;
    private DBCollection cpuStatsCollection;
    private DBCollection memoryStatsCollection;

    private Set<MemoryType> toDisplay = new HashSet<MemoryType>();

    public HostPanelFacadeImpl(HostRef ref, DB db) {
        this.agent = ref;
        this.db = db;

        hostInfoCollection = db.getCollection("host-info");
        networkInfoCollection = db.getCollection("network-info");
        cpuStatsCollection = db.getCollection("cpu-stats");
        memoryStatsCollection = db.getCollection("memory-stats");

        toDisplay.addAll(Arrays.asList(MemoryType.values()));
    }

    /*
     * Host-related methods
     */

    @Override
    public HostInfo getHostInfo() {
        DBObject hostInfo = hostInfoCollection.findOne(new BasicDBObject("agent-id", agent.getAgentId()));
        String hostName = (String) hostInfo.get("hostname");
        String osName = (String) hostInfo.get("os_name");
        String osKernel = (String) hostInfo.get("os_kernel");
        String cpuModel = (String) hostInfo.get("cpu_model");
        String cpuCount = (String) hostInfo.get("cpu_num");
        String memoryTotal = (String) hostInfo.get("memory_total");
        return new HostInfo(hostName, osName, osKernel, cpuModel, Integer.valueOf(cpuCount), Long.valueOf(memoryTotal));
    }

    @Override
    public NetworkInfo getNetworkInfo() {
        NetworkInfo network = new NetworkInfo();
        DBCursor cursor = networkInfoCollection.find(new BasicDBObject("agent-id", agent.getAgentId()));
        while (cursor.hasNext()) {
            DBObject iface = cursor.next();
            NetworkInterfaceInfo info = new NetworkInterfaceInfo((String) iface.get("iface"));
            if (iface.containsField("ipv4addr")) {
                info.setIp4Addr((String) iface.get("ipv4addr"));
            }
            if (iface.containsField("ipv6addr")) {
                info.setIp6Addr((String) iface.get("ipv6addr"));
            }
            network.addNetworkInterfaceInfo(info);
        }

        return network;
    }

    @Override
    public DiscreteTimeData<Double>[] getCpuLoad() {
        List<DiscreteTimeData<Double>> load = new ArrayList<DiscreteTimeData<Double>>();
        DBCursor cursor = cpuStatsCollection.find(new BasicDBObject("agent-id", agent.getAgentId()));
        long timestamp = 0;
        double data = 0;
        while (cursor.hasNext()) {
            DBObject stat = cursor.next();
            timestamp = Long.valueOf((String) stat.get("timestamp"));
            data = Double.valueOf((String) stat.get("5load"));
            load.add(new DiscreteTimeData<Double>(timestamp, data));
        }
        // TODO we may also want to avoid sending out thousands of values.
        // a subset of values from this entire array should suffice.
        return (DiscreteTimeData<Double>[]) load.toArray(new DiscreteTimeData<?>[0]);
    }

    @Override
    public DiscreteTimeData<Long>[] getMemoryUsage(MemoryType type) {
        List<DiscreteTimeData<Long>> data = new ArrayList<DiscreteTimeData<Long>>();
        DBCursor cursor = memoryStatsCollection.find(new BasicDBObject("agent-id", agent.getAgentId()));
        long timestamp = 0;
        long memoryData = 0;
        while (cursor.hasNext()) {
            DBObject stat = cursor.next();
            timestamp = Long.valueOf((String) stat.get("timestamp"));
            if (type.getInternalName().equals("used")) {
                memoryData = Long.valueOf((String) stat.get("total")) - Long.valueOf((String) stat.get("free"));
            } else {
                memoryData = Long.valueOf((String) stat.get(type.getInternalName()));
            }
            data.add(new DiscreteTimeData<Long>(timestamp, memoryData));
        }
        // TODO we may also want to avoid sending out thousands of values.
        // a subset of the values from this entire array should suffice.
        return (DiscreteTimeData<Long>[]) data.toArray(new DiscreteTimeData<?>[0]);
    }

    @Override
    public MemoryType[] getMemoryTypesToDisplay() {
        return toDisplay.toArray(new MemoryType[0]);
    }

    @Override
    public boolean isMemoryTypeDisplayed(MemoryType type) {
        return toDisplay.contains(type);
    }

    @Override
    public void setDisplayMemoryType(MemoryType type, boolean selected) {
        if (selected) {
            toDisplay.add(type);
        } else {
            toDisplay.remove(type);
        }
    }



}
