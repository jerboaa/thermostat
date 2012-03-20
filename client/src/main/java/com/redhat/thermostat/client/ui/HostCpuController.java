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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.AsyncUiFacade;
import com.redhat.thermostat.client.DiscreteTimeData;
import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.dao.CpuStatConverter;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;

public class HostCpuController implements AsyncUiFacade {

    private final HostRef hostRef;

    private final HostCpuView view;
    private final Timer backgroundUpdateTimer = new Timer();

    private final HostInfoDAO hostInfoDAO;
    private final DBCollection cpuStatsCollection;

    private long cpuLoadLastUpdateTime = Long.MIN_VALUE;

    public HostCpuController(HostRef ref, DB db) {
        hostRef = ref;

        view = createView();
        view.clearCpuLoadData();

        cpuStatsCollection = db.getCollection("cpu-stats");
        hostInfoDAO = ApplicationContext.getInstance().getDAOFactory().getHostInfoDAO(ref);
    }

    @Override
    public void start() {
        backgroundUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                HostInfo hostInfo = hostInfoDAO.getHostInfo();

                view.setCpuCount(String.valueOf(hostInfo.getCpuCount()));
                view.setCpuModel(String.valueOf(hostInfo.getCpuModel()));

                doCpuChartUpdate();
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public void stop() {
        backgroundUpdateTimer.cancel();
    }

    private void doCpuChartUpdate() {

        BasicDBObject query = new BasicDBObject();
        query.put("agent-id", this.hostRef.getAgentId());
        query.put("timestamp", new BasicDBObject("$gt", cpuLoadLastUpdateTime));

        DBCursor cursor = cpuStatsCollection.find(query);

        List<DiscreteTimeData<Double>> result = new ArrayList<DiscreteTimeData<Double>>();
        for (DBObject dbObj: cursor) {
                CpuStat stat = new CpuStatConverter().fromDB(dbObj);
                cpuLoadLastUpdateTime = Math.max(cpuLoadLastUpdateTime, stat.getTimeStamp());
                result.add(new DiscreteTimeData<Double>(stat.getTimeStamp(), stat.getLoad5()));
        }

        view.addCpuLoadData(result);
    }

    protected HostCpuView createView() {
        return new HostCpuPanel();
    }

    public Component getComponent() {
        return view.getUiComponent();
    }

}
