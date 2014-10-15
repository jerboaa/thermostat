/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.harvester;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ThreadBackend extends BaseBackend implements VmStatusListener {

    private static final Logger logger = LoggingUtils.getLogger(ThreadBackend.class);

    private final ReceiverRegistry registry;
    private final ThreadHarvester harvester;

    private boolean active = false;
    private VmStatusListenerRegistrar vmListener;
    private final List<Pair<String, Integer>> vmsToHarvestOnEnable = new ArrayList<>();

    public ThreadBackend(Version version, VmStatusListenerRegistrar registrar, ReceiverRegistry registry, ThreadHarvester harvester) {
        super("VM Thread Backend", "Gathers thread information about a JVM", "Red Hat, Inc", version.getVersionNumber());

        this.vmListener = registrar;
        this.registry = registry;
        this.harvester = harvester;
    }

    @Override
    public int getOrderValue() {
        return ORDER_THREAD_GROUP;
    }

    @Override
    public boolean activate() {
        if (active) {
            return true;
        }

        // bring back all harvesters that were active
        Iterator<Pair<String, Integer>> iter = vmsToHarvestOnEnable.iterator();
        while (iter.hasNext()) {
            Pair<String, Integer> saved = iter.next();
            harvester.startHarvester(saved.getFirst(), saved.getSecond());
            iter.remove();
        }

        vmListener.register(this);
        registry.registerReceiver(harvester);

        active = true;
        return true;
    }

    @Override
    public boolean deactivate() {
        if (!active) {
            return true;
        }
        vmListener.unregister(this);
        registry.unregisterReceivers();

        // stop all currently active harvesters
        vmsToHarvestOnEnable.addAll(harvester.stopAndRemoveAllHarvesters());

        active = false;
        return true;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        switch (newStatus) {
        case VM_STARTED: case VM_ACTIVE:
            /* this is blocking */
            harvester.addThreadHarvestingStatus(vmId);
            break;
        case VM_STOPPED:
            harvester.stopHarvester(vmId);
            break;
        default:
            logger.warning("Unexpected VM state: " + newStatus);
        }
    }

}

