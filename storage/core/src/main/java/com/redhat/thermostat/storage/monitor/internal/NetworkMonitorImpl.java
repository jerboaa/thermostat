/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.storage.monitor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;

public class NetworkMonitorImpl implements NetworkMonitor {
    
    public static final long DELAY = 1;

    protected final ActionNotifier<NetworkMonitor.Action> notifier;
    
    private Timer timer;
    private HostInfoDAO hostDAO;
    
    public NetworkMonitorImpl(TimerFactory timerFactory, HostInfoDAO hostDAO) {
        
        this.hostDAO = hostDAO;

        notifier = new ActionNotifier<>(this);
        
        timer = timerFactory.createTimer();
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setDelay(DELAY);
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        timer.setAction(new NetworkMonitorAction(notifier, hostDAO));
    }
    
    @Override
    public List<HostRef> getHosts(Filter<HostRef> matcher) {
        List<HostRef> hosts = new ArrayList<>();
        Collection<HostRef> _hosts = hostDAO.getHosts();
        for (HostRef host : _hosts) {
            if (matcher.matches(host)) {
                hosts.add(host);
            }
        }
        return hosts;
    }
    
    @Override
    public void addNetworkChangeListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
        if (notifier.listenersCount() == 1) {
            timer.start();
        }
    }
    
    @Override
    public void removeNetworkChangeListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
        if (notifier.listenersCount() == 0) {
            timer.stop(); 
        }
    }
}

