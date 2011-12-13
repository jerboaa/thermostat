package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StringUtils;

public class HostInfoBuilder {

    private static final String MEMINFO_FILE = "/proc/meminfo";
    private static final String CPUINFO_FILE = "/proc/cpuinfo";

    private static final Logger logger = LoggingUtils.getLogger(HostInfoBuilder.class);

    public HostInfo build() {
        InetAddress localAddr;
        String hostname;
        try {
            localAddr = InetAddress.getLocalHost();
            hostname = localAddr.getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostname = Constants.AGENT_LOCAL_HOSTNAME;
        }
        logger.log(Level.FINEST, "hostname: " + hostname);
        DistributionIdentity identifier = new DistributionIdentity();
        String osName = identifier.getName() + " " + identifier.getVersion();
        logger.log(Level.FINEST, "osName: " + osName);

        String osKernel = System.getProperty("os.name") + " " + System.getProperty("os.version");
        logger.log(Level.FINEST, "osKernel: " + osKernel);

        int cpuCount = getProcessorCountFromProc();
        logger.log(Level.FINEST, "cpuCount: " + cpuCount);

        long totalMemory = -1;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
            String[] memTotalParts = reader.readLine().split(" +");
            long data = Long.valueOf(memTotalParts[1]);
            String units = memTotalParts[2];
            if (units.equals("kB")) {
                totalMemory = data * Constants.KILOBYTES_TO_BYTES;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read /proc/meminfo");
        }
        logger.log(Level.FINEST, "totalMemory: " + totalMemory + " bytes");

        HashMap<String, List<String>> networkInfo = new HashMap<String, List<String>>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(ifaces)) {
                List<String> ipAddresses = new ArrayList<String>(2);
                ipAddresses.add(Constants.HOST_INFO_NETWORK_IPV4_INDEX, null);
                ipAddresses.add(Constants.HOST_INFO_NETWORK_IPV6_INDEX, null);
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        ipAddresses.set(Constants.HOST_INFO_NETWORK_IPV4_INDEX, fixAddr(addr.toString()));
                    } else if (addr instanceof Inet6Address) {
                        ipAddresses.set(Constants.HOST_INFO_NETWORK_IPV6_INDEX, fixAddr(addr.toString()));
                    }
                }
                networkInfo.put(iface.getName(), ipAddresses);
            }
        } catch (SocketException e) {
            logger.log(Level.WARNING, "error enumerating network interfaces");
        }

        return new HostInfo(hostname, osName, osKernel, cpuCount, totalMemory, networkInfo);

    }

    private int getProcessorCountFromProc() {
        final String KEY_PROCESSOR_ID = "processor";
        int totalCpus = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(CPUINFO_FILE));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(KEY_PROCESSOR_ID)) {
                    totalCpus++;
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read " + CPUINFO_FILE);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "unable to close " + CPUINFO_FILE);
                }
            }
        }
        return totalCpus;
    }

    /**
     * Removes the "hostname/" and the "%scope_id" parts from the
     * {@link InetAddress#toString()} output.
     */
    private String fixAddr(String addr) {
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
