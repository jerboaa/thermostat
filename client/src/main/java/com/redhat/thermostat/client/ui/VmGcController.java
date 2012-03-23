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

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.AsyncUiFacade;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.VmGcStat;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.dao.VmGcStatConverter;
import com.redhat.thermostat.common.dao.VmMemoryStatConverter;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.DiscreteTimeData;

public class VmGcController implements AsyncUiFacade {

    private final VmRef vmRef;
    private final VmGcView view;

    private final DBCollection vmGcStatsCollection;
    private final DBCollection vmMemoryStatsCollection;

    private final Map<String, Long> collectorSeriesLastUpdateTime = new HashMap<String, Long>();

    private final Set<String> addedCollectors = new TreeSet<String>();

    private final Timer timer = new Timer();

    public VmGcController(VmRef ref, DB db) {
        this.vmRef = ref;
        this.view = createView();

        vmGcStatsCollection = db.getCollection("vm-gc-stats");
        vmMemoryStatsCollection = db.getCollection("vm-memory-stats");
    }

    protected VmGcView createView() {
        return new VmGcPanel();
    }

    @Override
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doUpdateCollectorChartsAsync();

            }

        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public void stop() {
        for (String name: addedCollectors) {
            view.removeChart(name);
        }

        timer.cancel();
    }

    // FIXME
    private String chartName(String collectorName, String generationName) {
        return localize(LocaleResources.VM_GC_COLLECTOR_OVER_GENERATION,
                collectorName, generationName);
    }

    public String[] getCollectorNames() {
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("agent-id", vmRef.getAgent().getAgentId());
        queryObject.put("vm-id", Integer.valueOf(vmRef.getId()));
        @SuppressWarnings("unchecked")
        // This is temporary; this will eventually come from a DAO as the correct type.
        List<String> results = vmGcStatsCollection.distinct("collector", queryObject);
        List<String> collectorNames = new ArrayList<String>(results);

        return collectorNames.toArray(new String[0]);
    }

    private void doUpdateCollectorChartsAsync() {
        String[] collectorNames = getCollectorNames();
        for (final String name: collectorNames) {
            if (!addedCollectors.contains(name)) {
                view.addChart(name, chartName(name, getCollectorGeneration(name)));
                addedCollectors.add(name);
            }

            BasicDBObject queryObject = new BasicDBObject();
            queryObject.put("agent-id", vmRef.getAgent().getAgentId());
            queryObject.put("vm-id", Integer.valueOf(vmRef.getId()));
            queryObject.put("collector", name);
            Long lastTime = collectorSeriesLastUpdateTime.get(name);
            if (lastTime != null) {
                queryObject.put("timestamp", new BasicDBObject("$gt", (long) lastTime));
            }

            DBCursor cursor = vmGcStatsCollection.find(queryObject);

            List<DiscreteTimeData<? extends Number>> toAdd = new ArrayList<>();
            long lastUpdateTime = Long.MIN_VALUE;
            for (DBObject dbObj: cursor) {
                VmGcStat stat = new VmGcStatConverter().fromDBObject(dbObj);
                lastUpdateTime = Math.max(lastUpdateTime, stat.getTimeStamp());
                // convert microseconds to seconds
                double walltime = 1.0E-6 * stat.getWallTime();
                toAdd.add(new DiscreteTimeData<Double>(stat.getTimeStamp(), walltime));

            }
            view.addData(name, toAdd);
            collectorSeriesLastUpdateTime.put(name, lastUpdateTime);
        }
    }

    private VmMemoryStat getLatestMemoryStat() {
        BasicDBObject query = new BasicDBObject();
        query.put("agent-id", vmRef.getAgent().getAgentId());
        query.put("vm-id", Integer.valueOf(vmRef.getId()));
        // TODO ensure timestamp is an indexed column
        BasicDBObject sortByTimeStamp = new BasicDBObject("timestamp", -1);
        DBCursor cursor;
        cursor = vmMemoryStatsCollection.find(query).sort(sortByTimeStamp).limit(1);
        if (cursor.hasNext()) {
            DBObject current = cursor.next();
            return new VmMemoryStatConverter().createVmMemoryStatFromDBObject(current);
        }

        return null;
    }

    public String getCollectorGeneration(String collectorName) {
        VmMemoryStat info = getLatestMemoryStat();

        for (Generation g: info.getGenerations()) {
            if (g.collector.equals(collectorName)) {
                return g.name;
            }
        }
        return localize(LocaleResources.UNKNOWN_GEN);
    }

    /**
     * @return
     */
    public Component getComponent() {
        return view.getUiComponent();
    }

}
