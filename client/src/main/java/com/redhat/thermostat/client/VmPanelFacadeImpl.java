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

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;

public class VmPanelFacadeImpl implements VmPanelFacade {

    private final VmRef ref;
    private final DBCollection vmInfoCollection;
    private final DBCollection vmGcStatsCollection;
    private final DBCollection vmMemoryStatsCollection;

    private final ChangeableText vmPid = new ChangeableText("");
    private final ChangeableText startTime = new ChangeableText("");
    private final ChangeableText stopTime = new ChangeableText("");
    private final ChangeableText javaVersion = new ChangeableText("");
    private final ChangeableText javaHome = new ChangeableText("");
    private final ChangeableText mainClass = new ChangeableText("");
    private final ChangeableText commandLine = new ChangeableText("");
    private final ChangeableText vmName = new ChangeableText("");
    private final ChangeableText vmInfo = new ChangeableText("");
    private final ChangeableText vmVersion = new ChangeableText("");
    private final ChangeableText vmArguments = new ChangeableText("");
    private final ChangeableText vmNameAndVersion = new ChangeableText("");

    private final DefaultCategoryDataset currentMemoryDataset = new DefaultCategoryDataset();

    private final Map<String, TimeSeriesCollection> collectorSeriesCollection = new HashMap<String, TimeSeriesCollection>();
    private final Map<String, TimeSeries> collectorSeries = new HashMap<String, TimeSeries>();
    private final Map<String, Long> collectorSeriesLastUpdateTime = new HashMap<String, Long>();

    private final Timer timer = new Timer();

    private final DateFormat vmRunningTimeFormat;

    private VmMemoryStat cached;

    public VmPanelFacadeImpl(VmRef vmRef, DB db) {
        this.ref = vmRef;
        vmInfoCollection = db.getCollection("vm-info");
        vmGcStatsCollection = db.getCollection("vm-gc-stats");
        vmMemoryStatsCollection = db.getCollection("vm-memory-stats");

        vmRunningTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);
    }

    @Override
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                BasicDBObject queryObject = new BasicDBObject();
                queryObject.put("agent-id", ref.getAgent().getAgentId());
                queryObject.put("vm-id", Integer.valueOf(ref.getId()));
                DBObject vmInfoObject = vmInfoCollection.findOne(queryObject);
                vmPid.setText(((Integer) vmInfoObject.get("vm-pid")).toString());
                long actualStartTime = (Long) vmInfoObject.get("start-time");
                startTime.setText(vmRunningTimeFormat.format(new Date(actualStartTime)));
                long actualStopTime = (Long) vmInfoObject.get("stop-time");
                if (actualStopTime >= actualStartTime) {
                    // Only show a stop time if we have actually stopped.
                    stopTime.setText(vmRunningTimeFormat.format(new Date(actualStopTime)));
                } else {
                    stopTime.setText(localize(LocaleResources.VM_INFO_RUNNING));
                }
                javaVersion.setText((String) vmInfoObject.get("runtime-version"));
                javaHome.setText((String) vmInfoObject.get("java-home"));
                mainClass.setText((String) vmInfoObject.get("main-class"));
                commandLine.setText((String) vmInfoObject.get("command-line"));
                String actualVmName = (String) vmInfoObject.get("vm-name");
                vmName.setText(actualVmName);
                vmInfo.setText((String) vmInfoObject.get("vm-info"));
                String actualVmVersion = (String) vmInfoObject.get("vm-version");
                vmVersion.setText(actualVmVersion);
                vmArguments.setText((String) vmInfoObject.get("vm-arguments"));
                vmNameAndVersion.setText(localize(LocaleResources.VM_INFO_VM_NAME_AND_VERSION,
                                                  actualVmName, actualVmVersion));

                String[] collectorNames = getCollectorNames();
                for (String collectorName: collectorNames) {
                    TimeSeriesCollection seriesCollection = collectorSeriesCollection.get(collectorName);
                    if (seriesCollection == null) {
                        seriesCollection = new TimeSeriesCollection();
                        collectorSeriesCollection.put(collectorName, seriesCollection);
                    }
                    TimeSeries series = collectorSeries.get(collectorName);
                    if (series == null) {
                        series = new TimeSeries(collectorName);
                        collectorSeries.put(collectorName, series);
                    }
                    if (seriesCollection.getSeries(collectorName) == null) {
                        seriesCollection.addSeries(series);
                    }
                    if (!collectorSeriesLastUpdateTime.containsKey(collectorName)) {
                        collectorSeriesLastUpdateTime.put(collectorName, Long.MIN_VALUE);
                    }
                }

                doUpdateCurrentMemoryChartAsync();
                doUpdateCollectorChartsAsync();

            }

        }, 0, TimeUnit.SECONDS.toMillis(5));

    }

    @Override
    public void stop() {
        timer.cancel();
    }

    @Override
    public ChangeableText getVmPid() {
        return vmPid;
    }

    @Override
    public ChangeableText getJavaCommandLine() {
        return commandLine;
    }

    @Override
    public ChangeableText getJavaVersion() {
        return javaVersion;
    }

    @Override
    public ChangeableText getMainClass() {
        return mainClass;
    }

    @Override
    public ChangeableText getStartTimeStamp() {
        return startTime;
    }

    @Override
    public ChangeableText getStopTimeStamp() {
        return stopTime;
    }

    @Override
    public ChangeableText getVmNameAndVersion() {
        return vmNameAndVersion;
    }

    @Override
    public ChangeableText getVmName() {
        return vmName;
    }

    @Override
    public ChangeableText getVmVersion() {
        return vmVersion;
    }

    @Override
    public ChangeableText getVmArguments() {
    	return vmArguments;
    }

    @Override
    public String[] getCollectorNames() {
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("agent-id", ref.getAgent().getAgentId());
        queryObject.put("vm-id", Integer.valueOf(ref.getId()));
        List results = vmGcStatsCollection.distinct("collector", queryObject);
        List<String> collectorNames = new ArrayList<String>(results);

        return collectorNames.toArray(new String[0]);
    }

    private void doUpdateCollectorChartsAsync() {
        String[] collectorNames = getCollectorNames();
        for (String name: collectorNames) {
            CollectorChartUpdater updater = new CollectorChartUpdater(this, name);
            updater.execute();
        }
    }

    public static class CollectorChartUpdater extends SwingWorker<DiscreteTimeData<Double>[], Void> {

        private VmPanelFacadeImpl facade;
        private String collectorName;

        public CollectorChartUpdater(VmPanelFacadeImpl facade, String collectorName) {
            this.facade = facade;
            this.collectorName = collectorName;
        }

        @Override
        protected DiscreteTimeData<Double>[] doInBackground() throws Exception {
            Long after = facade.collectorSeriesLastUpdateTime.get(collectorName);
            if (after == null) {
                after = Long.MIN_VALUE;
            }
            return getCollectorRunTime(after);
        }

        private DiscreteTimeData<Double>[] getCollectorRunTime(long after) {
            ArrayList<DiscreteTimeData<Double>> result = new ArrayList<DiscreteTimeData<Double>>();
            BasicDBObject queryObject = new BasicDBObject();
            queryObject.put("agent-id", facade.ref.getAgent().getAgentId());
            queryObject.put("vm-id", Integer.valueOf(facade.ref.getId()));
            if (after != Long.MIN_VALUE) {
                // TODO once we have an index and the 'column' is of type long, use a
                // query which can utilize an index. this one doesn't
                queryObject.put("$where", "this.timestamp > " + after);
            }
            queryObject.put("collector", collectorName);
            DBCursor cursor = facade.vmGcStatsCollection.find(queryObject);
            long timestamp;
            double walltime;
            while (cursor.hasNext()) {
                DBObject current = cursor.next();
                timestamp = (Long) current.get("timestamp");
                // convert microseconds to seconds
                walltime = 1.0E-6 * (Long) current.get("wall-time");
                result.add(new DiscreteTimeData<Double>(timestamp, walltime));
            }

            return (DiscreteTimeData<Double>[]) result.toArray(new DiscreteTimeData<?>[0]);
        }

        @Override
        protected void done() {
            try {
                Long after = facade.collectorSeriesLastUpdateTime.get(collectorName);
                if (after == null) {
                    after = Long.MIN_VALUE;
                }
                after = appendCollectorDataToChart(get(), facade.collectorSeries.get(collectorName), after);
                facade.collectorSeriesLastUpdateTime.put(collectorName, after);
            } catch (ExecutionException ee) {
                ee.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        private long appendCollectorDataToChart(DiscreteTimeData<Double>[] collectorData, TimeSeries collectorSeries, long prevMaxTime) {
            long maxTime = prevMaxTime;

            if (collectorData.length > 0) {

                /*
                 * We have lots of new data to add. we do it in 2 steps:
                 * 1. Add everything with notify off.
                 * 2. Notify the chart that there has been a change. It
                 * does all the expensive computations and redraws itself.
                 */

                DiscreteTimeData<Double> data;
                for (int i = 0; i < collectorData.length; i++) {
                    data = collectorData[i];
                    maxTime = Math.max(maxTime, data.getTimeInMillis());
                    collectorSeries.add(
                            new FixedMillisecond(data.getTimeInMillis()), data.getData(),
                            /* notify = */false);
                }

                collectorSeries.fireSeriesChanged();
            }

            return maxTime;
        }
    }

    @Override
    public TimeSeriesCollection getCollectorDataSet(String collectorName) {
        TimeSeriesCollection seriesCollection = collectorSeriesCollection.get(collectorName);
        if (seriesCollection == null) {
            seriesCollection = new TimeSeriesCollection();
            collectorSeriesCollection.put(collectorName, seriesCollection);
        }
        return seriesCollection;
    }

    @Override
    public String getCollectorGeneration(String collectorName) {
        if (cached == null) {
            getLatestMemoryInfo();
        }
        for (Generation g: cached.getGenerations()) {
            if (g.collector.equals(collectorName)) {
                return g.name;
            }
        }
        return localize(LocaleResources.UNKNOWN_GEN);
    }

    private void doUpdateCurrentMemoryChartAsync() {
        UpdateCurrentMemory worker = new UpdateCurrentMemory(this);
        worker.execute();
    }

    private static class UpdateCurrentMemory extends SwingWorker<VmMemoryStat, Void> {

        private final VmPanelFacadeImpl facade;

        public UpdateCurrentMemory(VmPanelFacadeImpl facade) {
            this.facade = facade;
        }

        @Override
        protected VmMemoryStat doInBackground() throws Exception {
            return facade.getLatestMemoryInfo();
        }

        @Override
        protected void done() {
            try {
                VmMemoryStat info = get();
                DefaultCategoryDataset dataset = facade.currentMemoryDataset;
                List<Generation> generations = info.getGenerations();
                for (Generation generation: generations) {
                    List<Space> spaces = generation.spaces;
                    for (Space space: spaces) {
                        dataset.addValue(space.used, localize(LocaleResources.VM_CURRENT_MEMORY_CHART_USED), space.name);
                        dataset.addValue(space.capacity - space.used,
                                         localize(LocaleResources.VM_CURRENT_MEMORY_CHART_CAPACITY), space.name);
                        dataset.addValue(space.maxCapacity - space.capacity,
                                         localize(LocaleResources.VM_CURRENT_MEMORY_CHART_MAX_CAPACITY), space.name);
                    }
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (ExecutionException ee) {
                ee.printStackTrace();
            }
        }

    }

    private VmMemoryStat getLatestMemoryInfo() {
        BasicDBObject query = new BasicDBObject();
        query.put("agent-id", ref.getAgent().getAgentId());
        query.put("vm-id", Integer.valueOf(ref.getId()));
        // TODO ensure timestamp is an indexed column
        BasicDBObject sortByTimeStamp = new BasicDBObject("timestamp", -1);
        DBCursor cursor;
        cursor = vmMemoryStatsCollection.find(query).sort(sortByTimeStamp).limit(1);
        if (cursor.hasNext()) {
            DBObject current = cursor.next();
            return createVmMemoryStatFromDBObject(current);
        }

        return null;
    }

    private VmMemoryStat createVmMemoryStatFromDBObject(DBObject dbObj) {
        // FIXME so much hardcoding :'(

        String[] spaceNames = new String[] { "eden", "s0", "s1", "old", "perm" };
        List<Generation> generations = new ArrayList<Generation>();

        long timestamp = (Long) dbObj.get("timestamp");
        int vmId = (Integer) dbObj.get("vm-id");
        for (String spaceName: spaceNames) {
            DBObject info = (DBObject) dbObj.get(spaceName);
            String generationName = (String) info.get("gen");
            Generation target = null;
            for (Generation generation: generations) {
                if (generation.name.equals(generationName)) {
                    target = generation;
                }
            }
            if (target == null) {
                target = new Generation();
                target.name = generationName;
                generations.add(target);
            }
            if (target.collector == null) {
                target.collector = (String) info.get("collector");
            }
            Space space = new Space();
            space.name = spaceName;
            space.capacity = (Long) info.get("capacity");
            space.maxCapacity = (Long)info.get("max-capacity");
            space.used = (Long) info.get("used");
            if (target.spaces == null) {
                target.spaces = new ArrayList<Space>();
            }
            target.spaces.add(space);
        }

        cached = new VmMemoryStat(timestamp, vmId , generations);
        return cached;
    }

    @Override
    public DefaultCategoryDataset getCurrentMemory() {
        return currentMemoryDataset;
    }

}
