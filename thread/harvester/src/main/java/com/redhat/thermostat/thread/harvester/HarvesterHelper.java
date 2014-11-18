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

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadContentionSample;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/*
 */
class HarvesterHelper {

    private Clock clock;
    private String vmId;

    private ThreadSummaryHelper summaryHelper;
    private ThreadHeaderHelper headerHelper;
    private ThreadStateHelper stateHelper;
    private ThreadContentionHelper contentionHelper;
    private ThreadSessionHelper sessionHelper;

    HarvesterHelper(ThreadDao threadDao, Clock clock, String vmId, WriterID writerId)
    {
        this(threadDao, clock, vmId,
             new ThreadSummaryHelper(threadDao, writerId, vmId),
             new ThreadHeaderHelper(threadDao, writerId, vmId),
             new ThreadStateHelper(threadDao, writerId, vmId),
             new ThreadContentionHelper(threadDao, writerId, vmId),
             new ThreadSessionHelper(threadDao, writerId, vmId, clock));
    }

    HarvesterHelper(ThreadDao threadDao, Clock clock, String vmId,
                    ThreadSummaryHelper summaryHelper,
                    ThreadHeaderHelper headerHelper,
                    ThreadStateHelper stateHelper,
                    ThreadContentionHelper contentionHelper,
                    ThreadSessionHelper sessionHelper)
    {
        this.vmId = vmId;
        this.clock = clock;

        this.summaryHelper = summaryHelper;
        this.headerHelper = headerHelper;
        this.stateHelper = stateHelper;

        this.contentionHelper = contentionHelper;
        this.sessionHelper = sessionHelper;
    }

    synchronized void collectAndSaveThreadData(ThreadSession session,
                                               ThreadMXBean collectorBean)
    {
        long timestamp = clock.getRealTimeMillis();

        ThreadSummary summary =
                summaryHelper.createThreadSummary(collectorBean,
                                                  timestamp,
                                                  session);
        summaryHelper.saveSummary(summary);

        // this two can't be null, but the check is there to allow for
        // nicer tests
        long [] ids = collectorBean.getAllThreadIds();
        if (ids == null) {
            return;
        }

        // same as above
        ThreadInfo[] threadInfos = collectorBean.getThreadInfo(ids, true, true);
        if (threadInfos == null) {
            return;
        }

        for (int i = 0; i < ids.length; i++) {

            ThreadInfo beanInfo = threadInfos[i];

            // common header
            ThreadHeader header = headerHelper.createThreadHeader(beanInfo, timestamp);
            header = headerHelper.checkAndSaveThreadHeader(header);

            // state information
            ThreadState state = stateHelper.createThreadState(header, beanInfo,
                                                              timestamp);
            stateHelper.saveThreadState(state);

            // contention information
            ThreadContentionSample contentionSample =
                    contentionHelper.createThreadContentionSample(header,
                                                                  beanInfo,
                                                                  timestamp);
            contentionHelper.saveContentionSample(contentionSample);
        }
    }

    public ThreadSession createSession() {
        return sessionHelper.createSession();
    }

    public void saveSession(ThreadSession sessionID) {
        sessionHelper.saveSession(sessionID);
    }
}
