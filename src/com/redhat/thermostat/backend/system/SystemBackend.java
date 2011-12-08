package com.redhat.thermostat.backend.system;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;

import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.MemoryStat;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class SystemBackend extends Backend {

    private static final String NAME = "system";
    private static final String DESCRIPTION = "gathers basic information from the system";
    private static final String VENDOR = "thermostat project";
    private static final String VERSION = "0.01";

    private static final Logger logger = LoggingUtils.getLogger(SystemBackend.class);

    private long procCheckInterval = 1000;

    private Timer timer = null;

    private HostIdentifier hostId = null;
    private MonitoredHost host = null;
    private JvmStatHostListener hostListener = new JvmStatHostListener();


    @Override
    protected void setConfigurationValue(String name, String value) {
        logger.log(Level.INFO, "configuring " + NAME + " not supported");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, String> getConfigurationMap() {
        throw new NotImplementedException("get configuration");
    }

    @Override
    public synchronized boolean activate() {
        if (timer != null) {
            return true;
        }

        HostInfo hostInfo = new HostInfoBuilder().build();
        storage.updateHostInfo(hostInfo);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                CpuStat cpuStat = new CpuStatBuilder().build();
                storage.addCpuStat(cpuStat);

                MemoryStat memoryStat = new MemoryStatBuilder().build();
                storage.addMemoryStat(memoryStat);
            }
        }, 0, procCheckInterval);

        try {
            hostId = new HostIdentifier((String) null);
            host = MonitoredHost.getMonitoredHost(hostId);
            hostListener.setStorage(storage);
            host.addHostListener(hostListener);
        } catch (MonitorException me) {
            logger.log(Level.WARNING , "problems with connecting jvmstat to local machien" , me);
        } catch (URISyntaxException use) {
            logger.log(Level.WARNING , "problems with connecting jvmstat to local machien" , use);
        }

        return true;
    }

    @Override
    public synchronized boolean deactivate() {
        if (timer == null) {
            return true;
        }

        timer.cancel();
        timer = null;

        try {
            host.removeHostListener(hostListener);
        } catch (MonitorException me) {
            logger.log(Level.INFO, "something went wront in jvmstat's listeningto this host");
        }
        host = null;
        hostId = null;

        return true;
    }

    @Override
    public synchronized boolean isActive() {
        return (timer != null);
    }

    @Override
    public String getConfigurationValue(String key) {
        // TODO Auto-generated method stub
        return null;
    }

}
