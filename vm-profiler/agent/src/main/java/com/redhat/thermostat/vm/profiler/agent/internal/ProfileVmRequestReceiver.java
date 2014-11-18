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

package com.redhat.thermostat.vm.profiler.agent.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;

public class ProfileVmRequestReceiver implements RequestReceiver, VmStatusListener {

    private static final Logger logger = LoggingUtils.getLogger(ProfileVmRequestReceiver.class);

    private ConcurrentHashMap<String, Integer> vmIdToPid = new ConcurrentHashMap<>();

    /** A pid that corresponds to an unknown */
    private static final int UNKNOWN_VMID = -1;

    private final VmProfiler profiler;

    public ProfileVmRequestReceiver(VmProfiler profiler) {
        this.profiler = profiler;
    }

    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        if (newStatus == Status.VM_ACTIVE || newStatus == Status.VM_STARTED) {
            // assert not already being profiled
            vmIdToPid.putIfAbsent(vmId, pid);
        } else {
            // FIXME disable profiler if active?
            vmIdToPid.remove(vmId, pid);
        }
    }

    @Override
    public Response receive(Request request) {
        final Response OK = new Response(ResponseType.OK);
        final Response ERROR = new Response(ResponseType.NOK);

        String value = request.getParameter(ProfileRequest.PROFILE_ACTION);
        String vmId = request.getParameter(ProfileRequest.VM_ID);

        int pid = getPid(vmId);
        if (pid == UNKNOWN_VMID) {
            logger.warning("Unknown vmId: " + vmId + ". Known vmIds are : " + vmIdToPid.keySet().toString());
            return ERROR;
        }

        switch (value) {
        case ProfileRequest.START_PROFILING:
            logger.info("Starting profiling " + pid);
            try {
                profiler.startProfiling(pid);
                return OK;
            } catch (Exception e) {
                logger.log(Level.INFO, "start profiling failed", e);
                return ERROR;
            }
            /* should not reach here */
        case ProfileRequest.STOP_PROFILING:
            logger.info("Stopping profiling " + pid);
            try {
                profiler.stopProfiling(pid);
                return OK;
            } catch (Exception e) {
                logger.log(Level.INFO, "stop profiling failed", e);
                return ERROR;
            }
            /* should not reach here */
        default:
            logger.warning("Unknown command: '" + value + "'");
            return ERROR;
        }
    }

    /** Return the pid or {@link #UNKNOWN_VMID} */
    private int getPid(String vmId) {
        Integer pid = vmIdToPid.get(vmId);
        if (pid == null) {
            return UNKNOWN_VMID;
        } else {
            return pid;
        }
    }

}
