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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.ui.HostOverviewController;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.dao.CpuStatConverter;
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

        TimeSeriesUpdater.LastUpdateTimeCallback callback = new TimeSeriesUpdater.LastUpdateTimeCallback() {

            @Override
            public void update(long lastUpdateTime) {
                HostPanelFacadeImpl.this.cpuLoadLastUpdateTime = lastUpdateTime;
            }
        };
        Iterable<DBObject> dataSource = new Iterable<DBObject>() {
            @Override
            public Iterator<DBObject> iterator() {
                BasicDBObject query = new BasicDBObject();
                query.put("agent-id", HostPanelFacadeImpl.this.agent.getAgentId());
                query.put("timestamp", new BasicDBObject("$gt", cpuLoadLastUpdateTime));
                return cpuStatsCollection.find(query);
            }
        };
        TimeSeriesUpdater.Converter<Double, DBObject> converter = new TimeSeriesUpdater.Converter<Double, DBObject>() {
            @Override
            public DiscreteTimeData<Double> convert(DBObject dbObj) {
                CpuStat stat = new CpuStatConverter().fromDB(dbObj);
                return new DiscreteTimeData<Double>(stat.getTimeStamp(), stat.getLoad5());
            }
        };

        new TimeSeriesUpdater<Double, DBObject>(dataSource, cpuLoadSeries, converter, callback).execute();
    }


}
