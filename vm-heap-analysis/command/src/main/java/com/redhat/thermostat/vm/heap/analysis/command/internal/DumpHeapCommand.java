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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;


public class DumpHeapCommand extends AbstractStateNotifyingCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final long TIMEOUT_MS = 5000L;

    private final DumpHeapHelper implementation;

    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDAO;
    private HeapDAO heapDAO;
    private RequestQueue queue;

    public DumpHeapCommand() {
        this(new DumpHeapHelper());
    }

    DumpHeapCommand(DumpHeapHelper impl) {
        this.implementation = impl;
    }

    @Override
    public void run(final CommandContext ctx) throws CommandException {
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        requireNonNull(heapDAO, translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        requireNonNull(queue, translator.localize(LocaleResources.REQUEST_QUEUE_UNAVAILABLE));

        VmArgument vmArgument = VmArgument.required(ctx.getArguments());
        final VmId vmId = vmArgument.getVmId();
        final VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
        final AgentId agentId = new AgentId(vmInfo.getAgentId());

        final CommandException[] ex = new CommandException[1];
        final Semaphore s = new Semaphore(0);

        Runnable successHandler = new Runnable() {
            @Override
            public void run() {
                String latestHeapId = getLatestHeapId(heapDAO, agentId, vmId);
                // latestHeapId may be null if last heap dump is actually not yet available in storage
                if (latestHeapId != null) {
                    ctx.getConsole().getOutput().println(translator.localize(LocaleResources.COMMAND_HEAP_DUMP_DONE,
                            latestHeapId).getContents());
                } else {
                    ctx.getConsole().getOutput().println(translator.localize(LocaleResources.COMMAND_HEAP_DUMP_DONE_NOID).getContents());
                }
                s.release();
            }
        };

        Runnable errorHandler = new Runnable() {
            public void run() {
                ex[0] = new CommandException(translator.localize(
                        LocaleResources.HEAP_DUMP_ERROR, vmInfo.getAgentId(), vmInfo.getVmId()));
                s.release();
            }
        };

        implementation.execute(vmInfoDAO, agentInfoDAO, agentId, vmId, queue, successHandler, errorHandler);
        
        try {
            // There are two reasons why s.tryAquire() could time out:
            //
            //   1) The agent is dead and the heap dump couldn't be completed
            //   2) The agent is still alive, but the heap dump is so large it is
            //       taking longer than the timeout value
            //
            // To account for case 2, periodically check if the agent is still alive
            // by checking the semaphore in a while loop.
            long timeout = getTimeoutVal();
            while (!s.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
                if (!agentInfoDAO.getAliveAgentIds().contains(agentId)) {
                    ex[0] = new CommandException(translator.localize(
                            LocaleResources.HEAP_DUMP_ERROR_AGENT_DEAD, vmInfo.getAgentId(), vmInfo.getVmId()));
                    s.release();
                }
            }
        } catch (InterruptedException e) {
            // Nothing to do here, just return ASAP.
        }

        if (ex[0] != null) {
            getNotifier().fireAction(ApplicationState.FAIL);
            throw ex[0];
        }
        getNotifier().fireAction(ApplicationState.SUCCESS);
    }

    // package-private for testing
    long getTimeoutVal() {
        return TIMEOUT_MS;
    }

    // FIXME: storage may actually return us outdated results which do not contain the latest
    // heap dump(s). This can result in an empty list being returned (which we signal here by
    // returning null), or can result in a second dump-heap command incorrectly echoing the same
    // heap dump ID as the immediately prior dump-heap command.
    // See discussion here: http://icedtea.classpath.org/pipermail/thermostat/2016-May/018753.html
    static String getLatestHeapId(HeapDAO heapDao, AgentId agentId, VmId vmId) {
        Collection<HeapInfo> heapInfos = heapDao.getAllHeapInfo(agentId, vmId);
        if (heapInfos.isEmpty()) {
            return null;
        }
        List<HeapInfo> sortedByLatest = new ArrayList<>(heapInfos);
        Collections.sort(sortedByLatest, new Comparator<HeapInfo>() {
            @Override
            public int compare(HeapInfo a, HeapInfo b) {
                return Long.compare(b.getTimeStamp(), a.getTimeStamp());
            }
        });
        HeapInfo latest = sortedByLatest.get(0);
        return latest.getHeapId();
    }

    public void setVmInfoDAO(VmInfoDAO vmInfoDAO) {
        this.vmInfoDAO = vmInfoDAO;
    }

    public void setAgentInfoDAO(AgentInfoDAO agentInfoDAO) {
        this.agentInfoDAO = agentInfoDAO;
    }

    public void setRequestQueue(RequestQueue queue) {
        this.queue = queue;
    }

    public void setHeapDAO(HeapDAO heapDAO) {
        this.heapDAO = heapDAO;
    }
}

