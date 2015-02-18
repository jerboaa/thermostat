/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.backend;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * A backend that reads data from /proc for the host.
 * <p>
 * Register this as a {@link Backend} *and* as a {@link VmStatusListener}
 */
public abstract class VmProcReadingBackend extends BaseBackend implements VmStatusListener {

    private static final Logger logger = LoggingUtils.getLogger(VmProcReadingBackend.class);

    static final long PROC_CHECK_INTERVAL = 1000; // TODO make this configurable.

    private final ScheduledExecutorService executor;
    private final VmStatusListenerRegistrar registrar;

    private final Map<Integer, String> pidsToMonitor = new ConcurrentHashMap<>();
    private boolean started;

    public VmProcReadingBackend(String name, String description, String vendor,
            Version version,
            ScheduledExecutorService executor,
            VmStatusListenerRegistrar registrar) {

        super(name, description, vendor,
                version.getVersionNumber(), true);

        this.executor = executor;
        this.registrar = registrar;
    }

    @Override
    public boolean activate() {
        if (!started) {
            registrar.register(this);

            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (Entry<Integer, String> entry : pidsToMonitor.entrySet()) {
                        int pid = entry.getKey();
                        String vmId = entry.getValue();
                        readAndProcessProcData(vmId, pid);
                    }
                }
            }, 0, PROC_CHECK_INTERVAL, TimeUnit.MILLISECONDS);

            started = true;
        }
        return started;
    }

    @Override
    public boolean deactivate() {
        if (started) {
            executor.shutdown();
            registrar.unregister(this);

            started = false;
        }
        return !started;
    }

    @Override
    public boolean isActive() {
        return started;
    }

    /*
     * Methods implementing VmStatusListener
     */
    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            if (getObserveNewJvm()) {
                pidsToMonitor.put(pid, vmId);
                vmStarted(vmId, pid);
            } else {
                logger.log(Level.FINE, "skipping new vm " + pid);
            }
            break;
        case VM_STOPPED:
            pidsToMonitor.remove(pid);
            vmStopped(vmId, pid);
            break;
        }
    }

    protected abstract void readAndProcessProcData(String vmId, int pid);

    /** A VM is now active. Either it started now, or it started before this backend did. */
    protected void vmStarted(String vmId, int pid) { /* do nothing */ }

    /** A VM is now inactive. Either it stopped, or this backend is asked to stop. */
    protected void vmStopped(String vmId, int pid) { /* do nothing */ }

}
