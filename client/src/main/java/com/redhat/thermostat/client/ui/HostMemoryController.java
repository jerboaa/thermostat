/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import java.awt.Component;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.AsyncUiFacade;
import com.redhat.thermostat.client.DiscreteTimeData;
import com.redhat.thermostat.client.MemoryType;
import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.client.ui.HostMemoryView.GraphVisibilityChangeListener;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.MemoryStat;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MemoryStatConverter;

public class HostMemoryController implements AsyncUiFacade {

    private final HostMemoryView view;

    private final HostRef hostRef;
    private final HostInfoDAO hostInfoDAO;
    private final DBCollection memoryStatsCollection;

    private final Timer backgroundUpdateTimer;

    private final Map<MemoryType, Long> lastUpdateTime = new HashMap<>();
    private final List<MemoryType> currentlyDisplayed = new CopyOnWriteArrayList<>();

    public HostMemoryController(HostRef hostRef, DB db) {
        this.hostRef = hostRef;

        currentlyDisplayed.addAll(Arrays.asList(MemoryType.values()));

        hostInfoDAO = ApplicationContext.getInstance().getDAOFactory().getHostInfoDAO(hostRef);
        memoryStatsCollection = db.getCollection("memory-stats");

        for (MemoryType type: MemoryType.values()) {
            lastUpdateTime.put(type, Long.MIN_VALUE);
        }

        view = createView();

        view.addListener(new ShowHideGraph());

        backgroundUpdateTimer = new Timer();

    }

    @Override
    public void start() {
        for (MemoryType type: currentlyDisplayed) {
            view.addMemoryChart(type.name(), type.toString());
            view.showMemoryChart(type.name());
        }

        backgroundUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                HostInfo hostInfo = hostInfoDAO.getHostInfo();

                view.setTotalMemory(String.valueOf(hostInfo.getTotalMemory()));
                doMemoryChartUpdate();
            }

        }, 0, TimeUnit.SECONDS.toMillis(5));

    }

    @Override
    public void stop() {
        backgroundUpdateTimer.cancel();
        for (MemoryType type: currentlyDisplayed) {
            view.removeMemoryChart(type.name());
        }
    }

    public Component getComponent() {
        return view.getUiComponent();
    }

    private HostMemoryView createView() {
        return new HostMemoryPanel();
    }

    private void doMemoryChartUpdate() {
        for (final MemoryType type: currentlyDisplayed) {
            BasicDBObject query = new BasicDBObject();
            query.put("agent-id", hostRef.getAgentId());
            query.put("timestamp", new BasicDBObject("$gt", lastUpdateTime.get(type)));
            DBCursor cursor = memoryStatsCollection.find(query);

            List<DiscreteTimeData<? extends Number>> toAdd = new LinkedList<>();
            long lastUpdate = Long.MIN_VALUE;

            for (DBObject toConvert: cursor) {
                MemoryStat stat = new MemoryStatConverter().dbObjectToMemoryStat(toConvert);
                long data = 0;
                switch (type) {
                    case MEMORY_FREE:
                        data = stat.getFree();
                        break;
                    case MEMORY_TOTAL:
                        data = stat.getTotal();
                        break;
                    case MEMORY_USED:
                        data = stat.getTotal() - stat.getFree();
                        break;
                    case SWAP_BUFFERS:
                        data = stat.getBuffers();
                        break;
                    case SWAP_FREE:
                        data = stat.getFree();
                        break;
                    case SWAP_TOTAL:
                        data = stat.getTotal();
                        break;

                }
                lastUpdate = Math.max(lastUpdate, stat.getTimeStamp());
                toAdd.add(new DiscreteTimeData<Long>(stat.getTimeStamp(), data));
            }
            view.addMemoryData(type.name(), toAdd);
            lastUpdateTime.put(type, lastUpdate);
        }
    }

    private class ShowHideGraph implements GraphVisibilityChangeListener {
        @Override
        public void show(String tag) {
            view.showMemoryChart(tag);
        }
        @Override
        public void hide(String tag) {
            view.hideMemoryChart(tag);
        }

    }

}
