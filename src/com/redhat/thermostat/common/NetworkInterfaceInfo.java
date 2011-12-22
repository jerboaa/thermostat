package com.redhat.thermostat.common;

public class NetworkInterfaceInfo {

    private String iFace;
    private String ip4Addr;
    private String ip6Addr;

    public NetworkInterfaceInfo(String iFace) {
        this.iFace = iFace;
        this.ip4Addr = null;
        this.ip6Addr = null;
    }

    public String getInterfaceName() {
        return iFace;
    }

    public String getIp4Addr() {
        return ip4Addr;
    }

    public void setIp4Addr(String newAddr) {
        ip4Addr = newAddr;
    }

    public void clearIp4Addr() {
        ip4Addr = null;
    }

    public String getIp6Addr() {
        return ip6Addr;
    }

    public void setIp6Addr(String newAddr) {
        ip6Addr = newAddr;
    }

    public void clearIp6Addr() {
        ip6Addr = null;
    }
}
