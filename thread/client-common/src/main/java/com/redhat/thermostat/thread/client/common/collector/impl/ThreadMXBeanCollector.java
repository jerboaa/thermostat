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

package com.redhat.thermostat.thread.client.common.collector.impl;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadContentionSample;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadMXBeanCollector implements ThreadCollector {
    
    private static final String CMD_CHANNEL_ACTION_NAME = "thread-harvester";
    private static final Logger logger = LoggingUtils.getLogger(ThreadMXBeanCollector.class);

    private AgentInfoDAO agentDao;
    private ThreadDao threadDao;
    private BundleContext context;
    private VmRef ref;

    public ThreadMXBeanCollector(BundleContext context, VmRef ref) {
        this.context = context;
        this.ref = ref;
    }

    public void setThreadDao(ThreadDao threadDao) {
        this.threadDao = threadDao;
    }

    public void setAgentInfoDao(AgentInfoDAO agentDao) {
        this.agentDao = agentDao;
    }

    Request createRequest() {
        HostRef targetHostRef = ref.getHostRef();
        
        InetSocketAddress target = agentDao.getAgentInformation(targetHostRef).getRequestQueueAddress();
        Request harvester = new Request(RequestType.RESPONSE_EXPECTED, target);

        harvester.setReceiver(HarvesterCommand.RECEIVER);
        
        return harvester;
    }
    
    @Override
    public boolean startHarvester() {
        
        Request harvester = createRequest();
        harvester.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.START.name());
        harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getVmId());
        harvester.setParameter(HarvesterCommand.VM_PID.name(), String.valueOf(ref.getPid()));
        
        return postAndWait(harvester);

    }

    @Override
    public boolean stopHarvester() {
        
        Request harvester = createRequest();
        harvester.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.STOP.name());
        harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getVmId());

        boolean result = postAndWait(harvester);
        return result;
    }

    @Override
    public boolean isHarvesterCollecting() {
        ThreadHarvestingStatus status = threadDao.getLatestHarvestingStatus(ref);
        if (status == null) {
            return false;
        }
        return status.isHarvesting();
    }
    
    @Override
    public ThreadSummary getLatestThreadSummary() {
        ThreadSummary summary = threadDao.loadLastestSummary(ref);
        if (summary == null) {
            // default to all 0
            summary = new ThreadSummary();
        }
        return summary;
    }

    @Override
    public Range<Long> getThreadStateRange(ThreadHeader thread) {

        Range<Long> result = null;

        ThreadState last = threadDao.getLastThreadState(thread);
        ThreadState first = threadDao.getFirstThreadState(thread);

        if (last != null && first != null) {
            result = new Range<>(first.getProbeStartTime(), last.getProbeEndTime());
        }

        return result;
    }

    @Override
    public List<ThreadSummary> getThreadSummary(long since) {
        List<ThreadSummary> summary = threadDao.loadSummary(ref, since);
        return summary;
    }

    @Override
    public Range<Long> getThreadStateTotalTimeRange() {
        return threadDao.getThreadStateTotalTimeRange(ref);
    }

    public List<ThreadState> getThreadStates(ThreadHeader thread, Range<Long> range) {
        return threadDao.getThreadStates(thread, range);
    }

    @Override
    public VmDeadLockData getLatestDeadLockData() {
        return threadDao.loadLatestDeadLockStatus(ref);
    }

    @Override
    public void requestDeadLockCheck() {
        Request harvester = createRequest();
        harvester.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.FIND_DEADLOCKS.name());
        harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getVmId());
        harvester.setParameter(HarvesterCommand.VM_PID.name(), String.valueOf(ref.getPid()));

        postAndWait(harvester);
    }

    @Override
    public ThreadContentionSample getLatestContentionSample(ThreadHeader thread) {
        return threadDao.getLatestContentionSample(thread);
    }

    private boolean postAndWait(Request harvester) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];

        harvester.addListener(new RequestResponseListener() {
            @Override
            public void fireComplete(Request request, Response response) {
                switch (response.getType()) {
                case OK:
                    result[0] = true;
                    break;
                default:
                    break;
                }
                latch.countDown();
            }
        });

        try {
            enqueueRequest(harvester);
            latch.await();
        } catch (CommandChannelException e) {
            logger.log(Level.WARNING, "Failed to enqueue request", e);
        } catch (InterruptedException ignore) {}
        return result[0];
    }

    @Override
    public List<ThreadHeader> getThreads() {
        return threadDao.getThreads(ref);
    }

    private void enqueueRequest(Request req) throws CommandChannelException {
        ServiceReference ref = context.getServiceReference(RequestQueue.class.getName());
        if (ref == null) {
            throw new CommandChannelException("Cannot access command channel");
        }
        RequestQueue queue = (RequestQueue) context.getService(ref);
        queue.putRequest(req);
        context.ungetService(ref);
    }

    @SuppressWarnings("serial")
    private class CommandChannelException extends Exception {

        public CommandChannelException(String message) {
            super(message);
        }

    }
}

