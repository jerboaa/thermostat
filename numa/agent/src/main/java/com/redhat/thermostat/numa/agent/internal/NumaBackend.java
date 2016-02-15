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

package com.redhat.thermostat.numa.agent.internal;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaHostInfo;
import com.redhat.thermostat.numa.common.NumaNodeStat;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.storage.core.WriterID;

public class NumaBackend extends BaseBackend {

    private static final Logger log = Logger.getLogger(NumaBackend.class.getName());

    private static final long NUMA_CHECK_INTERVAL_SECONDS = 1;
    
    private final NumaDAO numaDAO;
    private final ApplicationService appService;
    private final NumaCollector numaCollector;
    private final WriterID writerId;
    private boolean started;
    private Timer timer;

    public NumaBackend(ApplicationService appService, NumaDAO numaDAO, NumaCollector numaCollector, Version version, WriterID writerID) {
        super("NUMA Backend",
                "Gathers NUMA statistics about a host",
                "Red Hat, Inc.",
                version.getVersionNumber());
        this.appService = appService;
        this.numaDAO = numaDAO;
        this.numaCollector = numaCollector;
        this.writerId = writerID;
    }

    @Override
    public boolean activate() {
        int numNodes = numaCollector.getNumberOfNumaNodes();
        NumaHostInfo info = buildNumaHostInfo(numNodes);
        numaDAO.putNumberOfNumaNodes(info);

        TimerFactory timerFactory = appService.getTimerFactory();
        timer = timerFactory.createTimer();
        timer.setDelay(NUMA_CHECK_INTERVAL_SECONDS);
        timer.setInitialDelay(0);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setAction(new Runnable() {

            @Override
            public void run() {
                NumaNodeStat[] stats;
                try {
                    stats = numaCollector.collectData();
                    String wId = writerId.getWriterID();
                    NumaStat numaStat = new NumaStat(wId);
                    numaStat.setTimeStamp(System.currentTimeMillis());
                    numaStat.setNodeStats(stats);
                    numaDAO.putNumaStat(numaStat);
                } catch (IOException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                }
            }
        });
        timer.start();
        started = true;

        return true;
    }

    // package private for testing
    NumaHostInfo buildNumaHostInfo(int numNodes) {
        String wId = writerId.getWriterID();
        NumaHostInfo info = new NumaHostInfo(wId);
        info.setNumNumaNodes(numNodes);
        return info;
    }

    @Override
    public boolean deactivate() {
        started = false;
        timer.stop();
        return true;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP + 80;
    }

}

