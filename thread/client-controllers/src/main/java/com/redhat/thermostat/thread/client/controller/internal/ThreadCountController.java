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

package com.redhat.thermostat.thread.client.controller.internal;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.chart.LivingDaemonThreadDifferenceChart;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.ThreadCountView;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadSummary;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

class ThreadCountController extends CommonController {

    private LivingDaemonThreadDifferenceChart model;
    private ThreadCollector collector;
    
    public ThreadCountController(ThreadCountView view, ThreadCollector collector, Timer timer, AppCache cache) {
        super(timer, view, cache);
        
        this.collector = collector;
        model = new LivingDaemonThreadDifferenceChart("Living Threads vs. Daemon Threads", "time", "threads",
                                                      "Living Threads", "Daemon Threads");
        model.setMaximumItemCount(3600);

        timer.setAction(new ThreadInformationDataCollector());
    }
    
    class ThreadInformationDataCollector extends SessionCheckingAction {

        @Override
        protected boolean isSessionValid(SessionID session) {
            return true;
        }

        @Override
        protected boolean isNewSession(SessionID session) {
            return false;
        }

        @Override
        protected Range<Long> getTotalRange(SessionID session) {
            long now = System.currentTimeMillis();
            long lastHour = now - TimeUnit.HOURS.toMillis(1);
            return new Range<>(lastHour, now);
        }

        @Override
        protected SessionID getCurrentSessionID() {
            return null;
        }

        @Override
        protected SessionID getLastAvailableSessionID() {
            return null;
        }

        @Override
        protected void actionPerformed(SessionID session, Range<Long> range, Range<Long> totalRange) {
            updateLastSession();
            updateChart(range);
        }

        private void updateLastSession() {

            ThreadCountView view = (ThreadCountView) ThreadCountController.this.view;

            // load the very latest thread summary
            ThreadSummary latestSummary = collector.getLatestThreadSummary();
            Objects.requireNonNull(latestSummary);

            if (latestSummary.getTimeStamp() != 0) {
                view.setLiveThreads(Long.toString(latestSummary.getCurrentLiveThreads()));
                view.setDaemonThreads(Long.toString(latestSummary.getCurrentDaemonThreads()));
            }
        }

        private void updateChart(Range<Long> range) {
            ThreadCountView view = (ThreadCountView) ThreadCountController.this.view;
            boolean updateModel = false;

            List<ThreadSummary> summaries = collector.getThreadSummary(range);
            if (summaries.size() != 0) {
                for (ThreadSummary summary : summaries) {
                    model.addData(summary.getTimeStamp(),
                                  summary.getCurrentLiveThreads(),
                                  summary.getCurrentDaemonThreads());
                }
                updateModel = true;
            }

            if (updateModel) {
                view.updateLivingDaemonTimeline(model);
            }
        }
    }    
}

