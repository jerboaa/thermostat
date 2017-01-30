/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.common.portability.internal.linux;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Size.Unit;
import com.redhat.thermostat.common.portability.HostName;
import com.redhat.thermostat.common.portability.PortableHost;
import com.redhat.thermostat.common.portability.PortableMemoryStat;
import com.redhat.thermostat.common.portability.internal.UnimplementedError;
import com.redhat.thermostat.common.portability.linux.ProcDataSource;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class LinuxPortableHostImpl implements PortableHost {

    public static PortableHost INSTANCE = new LinuxPortableHostImpl(new ProcDataSource());

    private static final Logger logger = LoggingUtils.getLogger(LinuxPortableHostImpl.class);

    private static final String FALLBACK_LOCAL_HOSTNAME = "localhost";

    private final ProcDataSource dataSource;

    public static PortableHost createInstance() {
        return new LinuxPortableHostImpl(new ProcDataSource());
    }

    static class HostCpuInfo {
        final String model;
        final int count;

        HostCpuInfo(String model, int count) {
            this.count = count;
            this.model = model;
        }
    }

    static class HostOsInfo {
        final String kernel;
        final String distribution;

        HostOsInfo(String kernel, String distribution) {
            this.kernel = kernel;
            this.distribution = distribution;
        }
    }


    LinuxPortableHostImpl(ProcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    HostCpuInfo getCpuInfo() {
        final String KEY_PROCESSOR_ID = "processor";
        final String KEY_CPU_MODEL = "model name";
        int cpuCount = 0;
        String cpuModel = null;
        try (BufferedReader bufferedReader = new BufferedReader(dataSource.getCpuInfoReader())) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(KEY_PROCESSOR_ID)) {
                    cpuCount++;
                } else if (line.startsWith(KEY_CPU_MODEL)) {
                    cpuModel = line.substring(line.indexOf(":") + 1).trim();
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "unable to read cpu info");
        }

        logger.log(Level.FINEST, "cpuModel: " + cpuModel);
        logger.log(Level.FINEST, "cpuCount: " + cpuCount);

        return new HostCpuInfo(cpuModel, cpuCount);
    }

    Size getTotalMemorySize() {
        Size totalMemory = null;
        try (BufferedReader bufferedReader = new BufferedReader(dataSource.getMemInfoReader())) {
            String[] memTotalParts = bufferedReader.readLine().split(" +");
            long data = Long.valueOf(memTotalParts[1]);
            String units = memTotalParts[2];
            if (units.equals("kB")) {
                totalMemory = new Size(data, Size.Unit.KiB);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "unable to read memory info");
        }

        logger.log(Level.FINEST, "totalMemory: " + (totalMemory != null ? totalMemory.toString() : "(null)"));
        return totalMemory;
    }

    @Override
    public String getHostName() {
        String hostname = null;
        
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            hostname = getHostName(localAddress);
        } catch (UnknownHostException uhe) {
            logger.log(Level.WARNING, "unable to get hostname", uhe);
        }
        
        // if fails, try to get hostname without dns lookup
        if (hostname == null) {
            hostname = HostName.getLocalHostName();
        }
        
        // still null, use localhost
        if (hostname == null) {
            hostname = FALLBACK_LOCAL_HOSTNAME;

        }
        
        return hostname;
    }

    HostOsInfo getOsInfo(DistributionInformation distroInfo) {
        String osName = distroInfo.getName() + " " + distroInfo.getVersion();
        logger.log(Level.FINEST, "osName: " + osName);
        String osKernel = System.getProperty("os.name") + " " + System.getProperty("os.version");
        logger.log(Level.FINEST, "osKernel: " + osKernel);
        return new HostOsInfo(osKernel, osName);
    }

    private HostOsInfo getOsInfo() {
        DistributionInformation distroInfo = DistributionInformation.get();
        return  getOsInfo(distroInfo);
    }

    @Override
    public String getOSName() {
        return getOsInfo().distribution;
    }

    @Override
    public String getOSVersion() {
        return getOsInfo().kernel;
    }

    @Override
    public String getCPUModel() {
        return getCpuInfo().model;
    }

    @Override
    public int getCPUCount() {
        return getCpuInfo().count;
    }

    @Override
    public long getTotalMemory() {
        return (long) getTotalMemorySize().convertTo(Unit.B).getValue();
    }

    @Override
    public long getClockTicksPerSecond() {
        // TODO
        throw new UnimplementedError("getClockTicksPerSecond()");
    }

    @Override
    public PortableMemoryStat getMemoryStat() {
        // TODO
        throw new UnimplementedError("getMemoryStat()");
    }

    String getHostName(InetAddress localAddress) {
        String hostname = localAddress.getCanonicalHostName();
        logger.log(Level.FINEST, "hostname: " + hostname);
        return hostname;
    }

}

