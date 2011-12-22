package com.redhat.thermostat.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NetworkInfo {

    private Map<String, NetworkInterfaceInfo> interfaces = new HashMap<String, NetworkInterfaceInfo>();

    public NetworkInfo() {
        
    }

    public synchronized void addNetworkInterfaceInfo(NetworkInterfaceInfo info) {
        interfaces.put(info.getInterfaceName(), info);
    }

    public synchronized void removeNetworkInterfaceInfo(NetworkInterfaceInfo info) {
        interfaces.remove(info.getInterfaceName());
    }

    public Iterator<NetworkInterfaceInfo> getInterfacesIterator() {
        return interfaces.values().iterator();
    }
}
