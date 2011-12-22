package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class HostInfoBuilder {

    private static final String MEMINFO_FILE = "/proc/meminfo";
    private static final String CPUINFO_FILE = "/proc/cpuinfo";

    private static final Logger logger = LoggingUtils.getLogger(HostInfoBuilder.class);

    public static HostInfo build() {
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
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(MEMINFO_FILE));
            String[] memTotalParts = reader.readLine().split(" +");
            long data = Long.valueOf(memTotalParts[1]);
            String units = memTotalParts[2];
            if (units.equals("kB")) {
                totalMemory = data * Constants.KILOBYTES_TO_BYTES;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read " + MEMINFO_FILE);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "unable to close " + MEMINFO_FILE);
                }
            }
        }
        logger.log(Level.FINEST, "totalMemory: " + totalMemory + " bytes");

        return new HostInfo(hostname, osName, osKernel, cpuCount, totalMemory);

    }

    private static int getProcessorCountFromProc() {
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
}
