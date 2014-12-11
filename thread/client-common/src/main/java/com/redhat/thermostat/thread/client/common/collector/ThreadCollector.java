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

package com.redhat.thermostat.thread.client.common.collector;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import java.util.List;

public interface ThreadCollector {
    
    void setAgentInfoDao(AgentInfoDAO agentDao);
    void setThreadDao(ThreadDao threadDao);

    boolean startHarvester();
    boolean stopHarvester();
    boolean isHarvesterCollecting();

    /**
     * Returns the range of all known threads probes.
     */
    Range<Long> getThreadRange(SessionID session);

    /**
     * Returns a list of sessions recorded during sampling.
     */
    List<ThreadSession> getThreadSessions(Range<Long> range);

    /**
     * Returns the last sampling session ID.
     */
    SessionID getLastThreadSession();

    ThreadSummary getLatestThreadSummary(SessionID session);
    List<ThreadSummary> getThreadSummary(SessionID session, Range<Long> range);

    /**
     * Check for deadlocks. {@link #getLatestDeadLockData} needs to be called to
     * obtain the data.
     */
    void requestDeadLockCheck();

    /** Return the latest deadlock data */
    VmDeadLockData getLatestDeadLockData();

    void getThreadStates(SessionID session,
                         ResultHandler<ThreadState> handler,
                         Range<Long> range);
}

