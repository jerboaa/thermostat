/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.controller.impl;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineFactory;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineProbe;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadState;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ThreadTimelineController extends CommonController {

    private static final boolean _DEBUG_BLOCK_TIMING_ = false;

    private ThreadTimelineView view;
    private ThreadCollector collector;

    private volatile boolean stopLooping;

    public ThreadTimelineController(ThreadTimelineView view,
                                    ThreadCollector collector,
                                    Timer timer)
    {
        super(timer, view);
        this.view = view;
        this.collector = collector;

        timer.setAction(new ThreadTimelineControllerAction());
    }

    private class ThreadTimelineControllerAction implements Runnable {

        private Range<Long> range;
        private Range<Long> lastRange;
        private ThreadStateResultHandler threadStateResultHandler;
        private long lastUpdate;

        private SessionID lastSession;

        public ThreadTimelineControllerAction() {
            lastUpdate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
            threadStateResultHandler = new ThreadStateResultHandler();
        }

        private void resetState() {
            view.clear();
            lastRange = null;
            lastUpdate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
            threadStateResultHandler.knownStates.clear();
            threadStateResultHandler.key = new ThreadInfo();
        }

        @Override
        public void run() {
            // FIXME: show only the last sessions for now, support for
            // filtering over sessions will come later
            SessionID session = collector.getLastThreadSession();
            if (session == null) {
                // ok, no data, let's skip this round
                return;
            }

            if (lastSession == null ||
                !session.get().equals(lastSession.get()))
            {
                // since we only visualise one session at a time and this is
                // a new session, let's clear the view
                resetState();
            }
            lastSession = session;

            // get the full range of known timelines per vm
            Range<Long> totalRange = collector.getThreadRange(session);
            if (totalRange == null) {
                // this just means we don't have any data yet
                return;
            }

            if (!totalRange.equals(lastRange)) {
                view.setTotalRange(totalRange);
            }
            lastRange = totalRange;

            range = new Range<>(lastUpdate, totalRange.getMax());
            lastUpdate = totalRange.getMax();

            collector.getThreadStates(session,
                                      threadStateResultHandler,
                                      range);
        }
    }

    @Override
    protected void onViewVisible() {
        stopLooping = false;
    }

    @Override
    protected void onViewHidden() {
        stopLooping = true;
    }

    private class ThreadStateResultHandler implements ResultHandler<ThreadState> {
        private ThreadInfo key;
        private Set<ThreadInfo> knownStates;

        public ThreadStateResultHandler() {
            this.key = new ThreadInfo();
            knownStates = new HashSet<>();
        }

        @Override
        public boolean onResult(ThreadState state) {

            key.setName(state.getName());
            key.setId(state.getId());

            ThreadInfo info = new ThreadInfo(key);
            if (!knownStates.contains(key)) {
                view.addThread(info);
                knownStates.add(info);
            }

            TimelineProbe probe = TimelineFactory.createTimelineProbe(state);
            view.addProbe(info, probe);

            boolean _stopLooping = stopLooping;
            return !_stopLooping;
        }
    }
}

