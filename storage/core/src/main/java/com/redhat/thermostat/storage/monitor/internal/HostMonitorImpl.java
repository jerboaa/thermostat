/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.monitor.HostMonitor;

public class HostMonitorImpl implements HostMonitor {
    
    static final long DELAY = 200;
    
    private VmInfoDAO vmDao;
    private TimerFactory timerFactory;
    
    private Map<HostRef, Pair<Timer, ActionNotifier<HostMonitor.Action>>> listeners;
    
    public HostMonitorImpl(TimerFactory timerFactory, VmInfoDAO vmDao) {
        this.vmDao = vmDao;
        this.timerFactory = timerFactory;
        listeners = new ConcurrentHashMap<>();
    }
    
    Map<HostRef, Pair<Timer, ActionNotifier<HostMonitor.Action>>> getListeners() {
        return listeners;
    }
    
    @Override
    public List<VmRef> getVirtualMachines(HostRef host, Filter<VmRef> matcher) {
        List<VmRef> vms = new ArrayList<>();
        Collection<VmRef> _vms = vmDao.getVMs(host);
        for (VmRef vm : _vms) {
            if (matcher.matches(vm)) {
                vms.add(vm);
            }
        }
        return vms;
    }
    
    @Override
    public void addHostChangeListener(HostRef host,
                                      ActionListener<Action> listener)
    {
        Pair<Timer, ActionNotifier<HostMonitor.Action>> payload =
                listeners.get(host);
        if (payload == null) {
            ActionNotifier<Action> notifier = new ActionNotifier<>(this);
            Timer timer = timerFactory.createTimer();

            timer.setTimeUnit(TimeUnit.MILLISECONDS);
            timer.setDelay(DELAY);
            timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
            timer.setAction(new HostMonitorAction(notifier, vmDao, host));
            timer.start();
            
            payload = new Pair<>(timer, notifier);
            listeners.put(host, payload);
        }
        
        payload.getSecond().addActionListener(listener);
    }

    @Override
    public void removeHostChangeListener(HostRef host,
                                         ActionListener<Action> listener)
    {
        Pair<Timer, ActionNotifier<HostMonitor.Action>> payload =
                listeners.get(host);
        if (payload != null) {
            ActionNotifier<HostMonitor.Action> notifier = payload.getSecond();
            notifier.removeActionListener(listener);
            if (notifier.listenersCount() == 0) {
                payload.getFirst().stop();
                listeners.remove(host);
            }
        }
    }
}
