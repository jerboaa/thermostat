package com.redhat.thermostat.common;

import java.util.Collection;

import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class DefaultHostsVMsLoader implements HostsVMsLoader {

    private HostInfoDAO hostsDAO;
    private VmInfoDAO vmsDAO;
    private boolean liveHosts;
    
    /**
     * 
     * @param hostDAO
     * @param vmsDAO
     * @param liveHosts {@code true} if only alive agent documents should get retrieved.
     */
    public DefaultHostsVMsLoader(HostInfoDAO hostDAO, VmInfoDAO vmsDAO, boolean liveHosts) {
        this.hostsDAO = hostDAO;
        this.vmsDAO = vmsDAO;
        this.liveHosts = liveHosts;
    }
    
    @Override
    public Collection<HostRef> getHosts() {
        if (liveHosts) {
            return hostsDAO.getAliveHosts();
        } else {
            return hostsDAO.getHosts();
        }
    }

    @Override
    public Collection<VmRef> getVMs(HostRef host) {
        return vmsDAO.getVMs(host);
    }

}
