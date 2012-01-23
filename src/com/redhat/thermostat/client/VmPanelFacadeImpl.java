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

import static com.redhat.thermostat.client.Translate._;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;

public class VmPanelFacadeImpl implements VmPanelFacade {

    private VmRef ref;
    private DB db;
    private DBCollection vmInfoCollection;
    private DBCollection vmGcStatsCollection;
    private DBCollection vmMemoryStatsCollection;

    private VmMemoryStat cached;

    public VmPanelFacadeImpl(VmRef vmRef, DB db) {
        this.ref = vmRef;
        this.db = db;
        vmInfoCollection = db.getCollection("vm-info");
        vmGcStatsCollection = db.getCollection("vm-gc-stats");
        vmMemoryStatsCollection = db.getCollection("vm-memory-stats");

    }

    @Override
    public VmInfo getVmInfo() {
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("agent-id", ref.getAgent().getAgentId());
        queryObject.put("vm-id", ref.getId());
        DBObject vmInfoObject = vmInfoCollection.findOne(queryObject);
        int vmPid = Integer.valueOf((String) vmInfoObject.get("vm-pid"));
        long startTime = Long.valueOf((String) vmInfoObject.get("start-time"));
        long stopTime = Long.valueOf((String) vmInfoObject.get("stop-time"));
        String javaVersion = (String) vmInfoObject.get("runtime-version");
        String javaHome = (String) vmInfoObject.get("java-home");
        String mainClass = (String) vmInfoObject.get("main-class");
        String commandLine = (String) vmInfoObject.get("command-line");
        String vmName = (String) vmInfoObject.get("vm-name");
        String vmInfo = (String) vmInfoObject.get("vm-info");
        String vmVersion = (String) vmInfoObject.get("vm-version");
        String vmArguments = (String) vmInfoObject.get("vm-arguments");
        // TODO fill in these
        Map<String, String> properties = new HashMap<String, String>();
        Map<String, String> environment = new HashMap<String, String>();
        List<String> loadedNativeLibraries = new ArrayList<String>();
        return new VmInfo(vmPid, startTime, stopTime,
                javaVersion, javaHome,
                mainClass, commandLine,
                vmName, vmInfo, vmVersion, vmArguments,
                properties, environment, loadedNativeLibraries);

    }

    @Override
    public String[] getCollectorNames() {
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("agent-id", ref.getAgent().getAgentId());
        queryObject.put("vm-id", ref.getId());
        List results = vmGcStatsCollection.distinct("collector", queryObject);
        List<String> collectorNames = new ArrayList<String>(results);

        return collectorNames.toArray(new String[0]);
    }

    @Override
    public DiscreteTimeData<Long>[] getCollectorRunTime(String collectorName) {
        ArrayList<DiscreteTimeData<Long>> result = new ArrayList<DiscreteTimeData<Long>>();
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("agent-id", ref.getAgent().getAgentId());
        queryObject.put("vm-id", ref.getId());
        queryObject.put("collector", collectorName);
        DBCursor cursor = vmGcStatsCollection.find(queryObject);
        long timestamp;
        long walltime;
        while (cursor.hasNext()) {
            DBObject current = cursor.next();
            timestamp = Long.valueOf((String) current.get("timestamp"));
            walltime = Long.valueOf((String) current.get("wall-time"));
            result.add(new DiscreteTimeData<Long>(timestamp, walltime));
        }

        return (DiscreteTimeData<Long>[]) result.toArray(new DiscreteTimeData<?>[0]);
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
        return _("UNKNOWN_GEN");
    }

    @Override
    public VmMemoryStat getLatestMemoryInfo() {
        BasicDBObject query = new BasicDBObject();
        query.put("agent-id", ref.getAgent().getAgentId());
        query.put("vm-id", ref.getId());
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

        long timestamp = Long.valueOf((String)dbObj.get("timestamp"));
        int vmId = Integer.valueOf((String)dbObj.get("vm-id"));
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
            space.capacity = Long.valueOf((String)info.get("capacity"));
            space.maxCapacity = Long.valueOf((String)info.get("max-capacity"));
            space.used = Long.valueOf((String)info.get("used"));
            if (target.spaces == null) {
                target.spaces = new ArrayList<Space>();
            }
            target.spaces.add(space);
        }

        cached = new VmMemoryStat(timestamp, vmId , generations);
        return cached;
    }

}
