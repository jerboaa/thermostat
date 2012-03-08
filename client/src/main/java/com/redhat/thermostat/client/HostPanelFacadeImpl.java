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

package com.redhat.thermostat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.ui.HostOverviewController;
import com.redhat.thermostat.common.dao.HostRef;

public class HostPanelFacadeImpl implements HostPanelFacade {

    private final HostRef agent;
    private final DBCollection cpuStatsCollection;
    private final DBCollection memoryStatsCollection;

    private final ChangeableText hostName = new ChangeableText("");
    private final ChangeableText cpuModel = new ChangeableText("");
    private final ChangeableText cpuCount = new ChangeableText("");
    private final ChangeableText memoryTotal = new ChangeableText("");

    private final TimeSeriesCollection cpuLoadTimeSeriesCollection = new TimeSeriesCollection();
    private final TimeSeries cpuLoadSeries = new TimeSeries("cpu-time");
    private long cpuLoadLastUpdateTime = Long.MIN_VALUE;

    private final Timer backgroundUpdateTimer = new Timer();

    private Set<MemoryType> toDisplay = new HashSet<MemoryType>();

    private final HostOverviewController overviewController;

    public HostPanelFacadeImpl(HostRef ref, DB db) {
        this.agent = ref;

        cpuStatsCollection = db.getCollection("cpu-stats");
        memoryStatsCollection = db.getCollection("memory-stats");

        cpuLoadTimeSeriesCollection.addSeries(cpuLoadSeries);

        toDisplay.addAll(Arrays.asList(MemoryType.values()));

        overviewController = new HostOverviewController(ref, db);
    }

    @Override
    public void start() {
        backgroundUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                doCpuChartUpdateAsync();
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));

        overviewController.start();
    }

    @Override
    public void stop() {
        backgroundUpdateTimer.cancel();

        overviewController.stop();
    }

    @Override
    public ChangeableText getHostName() {
        return hostName;
    }

    @Override
    public ChangeableText getCpuCount() {
        return cpuCount;
    }

    @Override
    public ChangeableText getCpuModel() {
        return cpuModel;
    }

    @Override
    public ChangeableText getTotalMemory() {
        return memoryTotal;
    }

    @Override
    public List<DiscreteTimeData<Long>> getMemoryUsage(MemoryType type) {
        List<DiscreteTimeData<Long>> data = new ArrayList<DiscreteTimeData<Long>>();
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("agent-id", agent.getAgentId());
        DBCursor cursor = memoryStatsCollection.find(queryObject);
        long timestamp = 0;
        long memoryData = 0;
        while (cursor.hasNext()) {
            DBObject stat = cursor.next();
            timestamp = (Long) stat.get("timestamp");
            if (type.getInternalName().equals("used")) {
                memoryData = (Long) stat.get("total") - (Long) stat.get("free");
            } else {
                memoryData = (Long) stat.get(type.getInternalName());
            }
            data.add(new DiscreteTimeData<Long>(timestamp, memoryData));
        }
        // TODO we may also want to avoid sending out thousands of values.
        // a subset of the values from this entire array should suffice.
        return data;
    }

    @Override
    public MemoryType[] getMemoryTypesToDisplay() {
        return toDisplay.toArray(new MemoryType[0]);
    }

    @Override
    public boolean isMemoryTypeDisplayed(MemoryType type) {
        return toDisplay.contains(type);
    }

    @Override
    public void setDisplayMemoryType(MemoryType type, boolean selected) {
        if (selected) {
            toDisplay.add(type);
        } else {
            toDisplay.remove(type);
        }
    }

    @Override
    public TimeSeriesCollection getCpuLoadDataSet() {
        return cpuLoadTimeSeriesCollection;
    }

    @Override
    public HostOverviewController getOverviewController() {
        return overviewController;
    }

    private void doCpuChartUpdateAsync() {
        CpuLoadChartUpdater updater = new CpuLoadChartUpdater(this);
        updater.execute();
    }

    private static class CpuLoadChartUpdater extends SwingWorker<List<DiscreteTimeData<Double>>, Void> {

        private HostPanelFacadeImpl facade;

        public CpuLoadChartUpdater(HostPanelFacadeImpl impl) {
            this.facade = impl;
        }

        @Override
        protected List<DiscreteTimeData<Double>> doInBackground() throws Exception {
            return getCpuLoad(facade.cpuLoadLastUpdateTime);
        }

        private List<DiscreteTimeData<Double>> getCpuLoad(long after) {
            List<DiscreteTimeData<Double>> load = new ArrayList<DiscreteTimeData<Double>>();
            BasicDBObject queryObject = new BasicDBObject();
            queryObject.put("agent-id", facade.agent.getAgentId());
            if (after != Long.MIN_VALUE) {
                // TODO once we have an index and the 'column' is of type long, use a
                // query which can utilize an index. this one doesn't
                queryObject.put("$where", "this.timestamp > " + after);
            }
            DBCursor cursor = facade.cpuStatsCollection.find(queryObject);
            long timestamp = 0;
            double data = 0;
            while (cursor.hasNext()) {
                DBObject stat = cursor.next();
                timestamp = (Long) stat.get("timestamp");
                data = (Double) stat.get("5load");
                load.add(new DiscreteTimeData<Double>(timestamp, data));
            }
            // TODO we may also want to avoid sending out thousands of values.
            // a subset of values from this entire array should suffice.
            return load;
        }

        @Override
        protected void done() {
            try {
                if (facade.cpuLoadLastUpdateTime == Long.MIN_VALUE) {
                    /* TODO clear stuff? */
                    facade.cpuLoadSeries.clear();
                }

                List<DiscreteTimeData<Double>> data = get();
                appendCpuChartData(data);

            } catch (ExecutionException ee) {
                ee.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        private void appendCpuChartData(List<DiscreteTimeData<Double>> cpuData) {
            if (cpuData.size() > 0) {

                /*
                 * We have lots of new data to add. we do it in 2 steps:
                 * 1. Add everything with notify off.
                 * 2. Notify the chart that there has been a change. It does
                 * all the expensive computations and redraws itself.
                 */
                for (DiscreteTimeData<Double> data: cpuData) {
                    facade.cpuLoadLastUpdateTime = Math.max(facade.cpuLoadLastUpdateTime, data.getTimeInMillis());
                    facade.cpuLoadSeries.add(
                            new FixedMillisecond(data.getTimeInMillis()), data.getData(),
                            /* notify = */false);
                }

                facade.cpuLoadSeries.fireSeriesChanged();
            }
        }

    }


}
