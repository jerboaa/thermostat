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

package com.redhat.thermostat.vm.gc.client.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.AgentIssue;
import com.redhat.thermostat.client.core.IssueDiagnoser;
import com.redhat.thermostat.client.core.Severity;
import com.redhat.thermostat.client.core.VmIssue;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;

public class VmGcIssueDiagnoser implements IssueDiagnoser {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private static final long GC_DURATION_THRESHOLD_MS = 250;

    private VmGcStatDAO gcDao;
    private Clock clock;

    public VmGcIssueDiagnoser(Clock clock, VmGcStatDAO gcDao) {
        this.clock = clock;
        this.gcDao = gcDao;
    }

    @Override
    public Collection<AgentIssue> diagnoseIssue(AgentId agentId) {
        return Collections.emptyList();
    }

    @Override
    public Collection<VmIssue> diagnoseIssue(AgentId agentId, VmId vmId) {
        List<VmGcStat> stats = fetchStatsForLastTenMinutes(agentId, vmId);
        return findIssuesInStats(agentId, vmId, stats);
    }

    private List<VmGcStat> fetchStatsForLastTenMinutes(AgentId agentId, VmId vmId) {
        long now = clock.getRealTimeMillis();
        long tenMinutesInMillis = TimeUnit.MINUTES.toMillis(10);
        long since = now - tenMinutesInMillis;
        List<VmGcStat> stats = new ArrayList<>(gcDao.getLatestVmGcStats(agentId, vmId, since));

        Collections.sort(stats, new Comparator<VmGcStat>() {
            @Override
            public int compare(VmGcStat o1, VmGcStat o2) {
                return (int) Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
            }
        });

        return stats;
    }

    private Collection<VmIssue> findIssuesInStats(AgentId agentId, VmId vmId, List<VmGcStat> stats) {
        List<VmIssue> issues = new ArrayList<>();

        issues.addAll(findLongGcTimes(agentId, vmId, stats));
        issues.addAll(findGcInvokedTooOften(agentId, vmId, stats));

        return issues;
    }

    private Collection<VmIssue> findLongGcTimes(AgentId agentId, VmId vmId, List<VmGcStat> stats) throws AssertionError {
        // collector -> max gc time
        Map<String, Long> maxGcTime = new HashMap<>();
        // collector -> timeStamp
        Map<String, Long> lastTimeStamp = new HashMap<>();
        // collector -> wallTime
        Map<String, Long> lastWallTimeInMicros = new HashMap<>();

        for (VmGcStat stat: stats) {
            String collectorName = stat.getCollectorName();
            long timeStamp = stat.getTimeStamp();
            long wallTimeInMicros = stat.getWallTime();

            if (lastTimeStamp.containsKey(collectorName)) {
                long previousTimeStamp = lastTimeStamp.get(collectorName);
                long previousWallTimeInMicros = lastWallTimeInMicros.get(collectorName);

                if (timeStamp <= previousTimeStamp) {
                    throw new AssertionError("TimeStamps not in order");
                }

                long gcTimeInMicros = wallTimeInMicros - previousWallTimeInMicros;
                long gcTimeInMillis = TimeUnit.MICROSECONDS.toMillis(gcTimeInMicros);
                Long previousMaxGcValue = maxGcTime.get(collectorName);
                long maxGcTimeInMillis = Math.max(previousMaxGcValue == null ? Long.MIN_VALUE : previousMaxGcValue, gcTimeInMillis);
                maxGcTime.put(collectorName, maxGcTimeInMillis);
            }

            lastTimeStamp.put(collectorName, timeStamp);
            lastWallTimeInMicros.put(collectorName, wallTimeInMicros);
        }

        List<VmIssue> issues = new ArrayList<>();
        for (Entry<String,Long> collectorToMaxGcTime : maxGcTime.entrySet()) {
            String collectorName = collectorToMaxGcTime.getKey();
            long maxGcTimeInMillis = collectorToMaxGcTime.getValue();
            if (maxGcTimeInMillis > GC_DURATION_THRESHOLD_MS) {
                VmIssue issue = new VmIssue(agentId,  vmId, Severity.WARNING,
                        translate.localize(LocaleResources.ISSUE_GC_TOOK_TOO_LONG, collectorName, String.valueOf(maxGcTimeInMillis) + "ms").getContents());
                issues.add(issue);
            }
        }
        return issues;
    }

    private Collection<VmIssue> findGcInvokedTooOften(AgentId agentId, VmId vmId, List<VmGcStat> stats) throws AssertionError {
        // TODO implement me
        return Collections.emptyList();
    }

}
