package com.redhat.thermostat.backend.system;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.NetworkInfo;
import com.redhat.thermostat.common.NetworkInterfaceInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class NetworkInfoBuilder {

    private static final Logger logger = LoggingUtils.getLogger(NetworkInfoBuilder.class);

    public static NetworkInfo build() {
        NetworkInfo info = new NetworkInfo();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(ifaces)) {
                NetworkInterfaceInfo iInfo = new NetworkInterfaceInfo(iface.getName());
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        iInfo.setIp4Addr(fixAddr(addr.toString()));
                    } else if (addr instanceof Inet6Address) {
                        iInfo.setIp6Addr(fixAddr(addr.toString()));
                    }
                }
                info.addNetworkInterfaceInfo(iInfo);
            }
        } catch (SocketException e) {
            logger.log(Level.WARNING, "error enumerating network interfaces");
        }
        return info;
    }

    /**
     * Removes the "hostname/" and the "%scope_id" parts from the
     * {@link InetAddress#toString()} output.
     */
    private static String fixAddr(String addr) {
        int slashPos = addr.indexOf("/");
        if (slashPos == -1) {
            return addr;
        }
        String fixed = addr.substring(slashPos + 1);
        int percentPos = fixed.indexOf("%");
        if (percentPos == -1) {
            return fixed;
        }
        fixed = fixed.substring(0, percentPos);
        return fixed;
    }
}
