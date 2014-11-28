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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileVmRequestReceiver implements RequestReceiver, VmStatusListener {

    private static final Logger logger = LoggingUtils.getLogger(ProfileVmRequestReceiver.class);

    /** A pid that corresponds to an unknown */
    private static final int UNKNOWN_VMID = -1;

    private static final Response OK = new Response(ResponseType.OK);
    private static final Response ERROR = new Response(ResponseType.NOK);

    static class FileTimeStampLatestFirst implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return Long.compare(o2.lastModified(), o1.lastModified());
        }
    }

    static class ProfileUploaderCreator {
        ProfileUploader create(ProfileDAO dao, String agentId, String vmId, int pid) {
            return new ProfileUploader(dao, agentId, vmId, pid);
        }
    }

    private final ConcurrentHashMap<String, Integer> vmIdToPid = new ConcurrentHashMap<>();

    private final String agentId;

    private final Clock clock;
    private final VmProfiler profiler;
    private final ProfileDAO dao;
    private ProfileUploaderCreator uploaderCreator;

    private List<Integer> currentlyProfiledVms = new ArrayList<>();

    public ProfileVmRequestReceiver(String agentId, VmProfiler profiler, ProfileDAO dao) {
        this(agentId, new SystemClock(), profiler, dao, new ProfileUploaderCreator());
    }

    public ProfileVmRequestReceiver(String agentId, Clock clock, VmProfiler profiler, ProfileDAO dao, ProfileUploaderCreator uploaderCreator) {
        this.clock = clock;
        this.profiler = profiler;
        this.dao = dao;
        this.uploaderCreator = uploaderCreator;

        this.agentId = agentId;
    }

    @Override
    public synchronized void vmStatusChanged(Status newStatus, String vmId, int pid) {
        if (newStatus == Status.VM_ACTIVE || newStatus == Status.VM_STARTED) {
            // TODO assert not already being profiled
            vmIdToPid.putIfAbsent(vmId, pid);
        } else {
            disableProfilerIfActive(vmId, pid);
            vmIdToPid.remove(vmId, pid);
        }
    }

    private void disableProfilerIfActive(String vmId, int pid) {
        if (currentlyProfiledVms.contains(pid)) {
            stopProfiling(vmId, pid, false);
        }
    }

    @Override
    public synchronized Response receive(Request request) {
        String value = request.getParameter(ProfileRequest.PROFILE_ACTION);
        String vmId = request.getParameter(ProfileRequest.VM_ID);

        int pid = getPid(vmId);
        if (pid == UNKNOWN_VMID) {
            logger.warning("Unknown vmId: " + vmId + ". Known vmIds are : " + vmIdToPid.keySet().toString());
            return ERROR;
        }

        switch (value) {
        case ProfileRequest.START_PROFILING:
            return startProfiling(vmId, pid);
        case ProfileRequest.STOP_PROFILING:
            return stopProfiling(vmId, pid, true);
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

    private Response startProfiling(String vmId, int pid) {
        logger.info("Starting profiling " + pid);
        try {
            profiler.startProfiling(pid);
            currentlyProfiledVms.add(pid);
            dao.addStatus(new ProfileStatusChange(agentId, vmId, clock.getRealTimeMillis(), true));
            return OK;
        } catch (Exception e) {
            logger.log(Level.INFO, "start profiling failed", e);
            return ERROR;
        }
    }

    private Response stopProfiling(String vmId, int pid, boolean alive) {
        logger.info("Stopping profiling " + pid);
        try {
            ProfileUploader uploader = uploaderCreator.create(dao, agentId, vmId, pid);
            if (alive) {
                // if the VM is alive, communicate with it directly
                profiler.stopProfiling(pid, uploader);
            } else {
                findAndUploadProfilingResultsStoredOnDisk(pid, uploader);
            }
            dao.addStatus(new ProfileStatusChange(agentId, vmId, clock.getRealTimeMillis(), false));
            currentlyProfiledVms.remove((Integer) pid);
            return OK;
        } catch (Exception e) {
            logger.log(Level.INFO, "stop profiling failed", e);
            return ERROR;
        }
    }

    private void findAndUploadProfilingResultsStoredOnDisk(final int pid, ProfileUploader uploader) throws IOException {
        long timeStamp = clock.getRealTimeMillis();
        // look for latest profiling data that it might have emitted on shutdown
        File file = findProfilingResultFile(pid);
        uploader.upload(timeStamp, file);
    }

    private File findProfilingResultFile(final int pid) {
        // from InstrumentationControl:
        // return Files.createTempFile("thermostat-" + getProcessId(), ".perfdata", attributes);
        String tmpDir = System.getProperty("java.io.tmpdir");
        File[] files = new File(tmpDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("thermostat-" + pid + "-") && name.endsWith(".perfdata");
            }
        });

        List<File> filesSortedByTimeStamp = Arrays.asList(files);
        Collections.sort(filesSortedByTimeStamp, new FileTimeStampLatestFirst());
        return filesSortedByTimeStamp.get(0);
    }

}
