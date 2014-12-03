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
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class VmProfiler {

    static class MostRecentFileFirst implements Comparator<File> {
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

    private static final Logger logger = LoggingUtils.getLogger(VmProfiler.class);

    private final List<Integer> vmsWithAgentLoaded = new ArrayList<>();
    private final List<Integer> currentlyProfiledVmPids = new ArrayList<>();

    private final VmIdToPidMapper vmIdToPid = new VmIdToPidMapper();

    private final String agentId;
    private final Clock clock;
    private final ProfileDAO dao;
    private final ProfileUploaderCreator uploaderCreator;
    private final RemoteProfilerCommunicator remote;

    private final String agentJarPath;
    private final String asmJarPath;

    public VmProfiler(String agentId, Properties configuration, ProfileDAO dao, MXBeanConnectionPool pool) {
        this(agentId, configuration, dao, new SystemClock(), new ProfileUploaderCreator(), new RemoteProfilerCommunicator(pool));
    }

    VmProfiler(String agentId, Properties configuration,
            ProfileDAO dao,
            Clock clock, ProfileUploaderCreator creator, RemoteProfilerCommunicator remote) {
        this.agentId = agentId;
        this.clock = clock;
        this.dao = dao;
        this.uploaderCreator = creator;
        this.remote = remote;

        // requireNonNull protects against bad config with missing values
        agentJarPath = Objects.requireNonNull(configuration.getProperty("AGENT_JAR"));
        asmJarPath = Objects.requireNonNull(configuration.getProperty("ASM_JAR"));
    }

    public synchronized void vmStarted(String vmId, int pid) {
        // assert not already being profiled
        if (currentlyProfiledVmPids.contains((Integer) pid)) {
            throw new IllegalStateException("VM " + pid + " is already being profiled");
        }
        vmIdToPid.add(vmId, pid);
    }

    public synchronized void vmStopped(String vmId, int pid) {
        try {
            disableProfilerIfActive(vmId, pid);
        } catch (ProfilerException e) {
            logger.warning(e.getMessage());
        }
        vmIdToPid.remove(vmId, pid);
        vmsWithAgentLoaded.remove((Integer)pid);
    }

    private void disableProfilerIfActive(String vmId, int pid) throws ProfilerException {
        if (currentlyProfiledVmPids.contains(pid)) {
            stopProfiling(vmId, false);
        }
    }

    public synchronized void startProfiling(String vmId) throws ProfilerException {
        int pid = vmIdToPid.getPid(vmId);
        if (pid == VmIdToPidMapper.UNKNOWN_VMID) {
            throw new ProfilerException("Unknown VmId " + vmId);
        }

        if (currentlyProfiledVmPids.contains((Integer) pid)) {
            throw new ProfilerException("Already profiling the VM");
        }

        if (!vmsWithAgentLoaded.contains((Integer)pid)) {
            // TODO make this adjustable at run-time
            // eg: asmJarPath + ":" + agentJarPath;
            String jarsToLoad = "";
            logger.info("Asking " + pid + " to load agent '" + agentJarPath + "' with arguments '" + jarsToLoad + "'");

            remote.loadAgentIntoPid(pid, agentJarPath, jarsToLoad);
            vmsWithAgentLoaded.add(pid);
        }

        remote.startProfiling(pid);

        currentlyProfiledVmPids.add(pid);
        dao.addStatus(new ProfileStatusChange(agentId, vmId, clock.getRealTimeMillis(), true));
    }

    public synchronized void stopProfiling(String vmId) throws ProfilerException {
        stopProfiling(vmId, true);
    }

    private void stopProfiling(String vmId, boolean alive) throws ProfilerException {
        int pid = vmIdToPid.getPid(vmId);
        if (pid == VmIdToPidMapper.UNKNOWN_VMID) {
            throw new ProfilerException("VmId not found: " + vmId);
        }

        if (!currentlyProfiledVmPids.contains(pid)) {
            throw new ProfilerException("Vm is not being profiled: " + vmId);
        }

        ProfileUploader uploader = uploaderCreator.create(dao, agentId, vmId, pid);
        if (alive) {
            stopRemoteProfilerAndUploadResults(pid, uploader);
        } else {
            findAndUploadProfilingResultsStoredOnDisk(pid, uploader);
        }
        dao.addStatus(new ProfileStatusChange(agentId, vmId, clock.getRealTimeMillis(), false));
        currentlyProfiledVmPids.remove((Integer) pid);
    }

    private void stopRemoteProfilerAndUploadResults(int pid, ProfileUploader uploader) throws ProfilerException {
        remote.stopProfiling(pid);

        String profilingDataFile = remote.getProfilingDataFile(pid);
        upload(uploader, clock.getRealTimeMillis(), new File(profilingDataFile));
    }

    private void findAndUploadProfilingResultsStoredOnDisk(final int pid, ProfileUploader uploader) throws ProfilerException {
        long timeStamp = clock.getRealTimeMillis();
        // look for latest profiling data that it might have emitted on shutdown
        File file = findProfilingResultFile(pid);
        upload(uploader, timeStamp, file);
    }

    private File findProfilingResultFile(final int pid) {
        // from InstrumentationControl:
        // return Files.createTempFile("thermostat-" + getProcessId() + "-", ".perfdata", attributes);
        String tmpDir = System.getProperty("java.io.tmpdir");
        File[] files = new File(tmpDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("thermostat-" + pid + "-") && name.endsWith(".perfdata");
            }
        });

        List<File> filesSortedByTimeStamp = Arrays.asList(files);
        Collections.sort(filesSortedByTimeStamp, new MostRecentFileFirst());
        return filesSortedByTimeStamp.get(0);
    }

    private void upload(ProfileUploader uploader, long timeStamp, File file) throws ProfilerException {
        try {
            uploader.upload(clock.getRealTimeMillis(), file);
        } catch (IOException e) {
            throw new ProfilerException("Unable to save profiling data into storage", e);
        }
    }

}
