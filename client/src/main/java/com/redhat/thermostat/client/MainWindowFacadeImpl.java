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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.ui.MainWindow;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class MainWindowFacadeImpl implements MainWindowFacade, HostsVMsLoader {

    private static final Logger logger = LoggingUtils.getLogger(MainWindowFacadeImpl.class);

    private final DBCollection agentConfigCollection;
    private final DBCollection hostInfoCollection;
    private final DBCollection vmInfoCollection;

    private final Timer backgroundUpdater = new Timer();

    private MainWindow view;

    private String filter;

    public MainWindowFacadeImpl(DB db) {
        this.agentConfigCollection = db.getCollection("agent-config");
        this.hostInfoCollection = db.getCollection("host-info");
        this.vmInfoCollection = db.getCollection("vm-info");
    }

    @Override
    public void start() {
        backgroundUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doUpdateTreeAsync();
            }
        }, 0, TimeUnit.SECONDS.toMillis(10));
    }

    @Override
    public void stop() {
        backgroundUpdater.cancel();
    }

    @Override
    public Collection<HostRef> getHosts() {
        List<HostRef> hostRefs = new ArrayList<HostRef>();

        DBCursor cursor = agentConfigCollection.find();
        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            String id = (String) doc.get("agent-id");
            if (id != null) {
                DBObject hostInfo = hostInfoCollection.findOne(new BasicDBObject("agent-id", id));
                String hostName = (String) hostInfo.get("hostname");
                HostRef agent = new HostRef(id, hostName);
                hostRefs.add(agent);
            }
        }
        logger.log(Level.FINER, "found " + hostRefs.size() + " connected agents");
        return hostRefs;
    }

    @Override
    public Collection<VmRef> getVMs(HostRef hostRef) {
        List<VmRef> vmRefs = new ArrayList<VmRef>();
        DBCursor cursor = vmInfoCollection.find(new BasicDBObject("agent-id", hostRef.getAgentId()));
        while (cursor.hasNext()) {
            DBObject vmObject = cursor.next();
            Integer id = (Integer) vmObject.get("vm-id");
            // TODO can we do better than the main class?
            String mainClass = (String) vmObject.get("main-class");
            VmRef ref = new VmRef(hostRef, id, mainClass);
            vmRefs.add(ref);
        }

        return vmRefs;
    }

    @Override
    public void setHostVmTreeFilter(String filter) {
        this.filter = filter;
        doUpdateTreeAsync();
    }

    public void doUpdateTreeAsync() {
        view.updateTree(filter, this);
    }

    @Override
    public void initView(MainWindow mainWindow) {
        this.view = mainWindow;
        mainWindow.addViewPropertyListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName(); 
                if (propertyName.equals(MainWindow.HOST_VM_TREE_FILTER_PROPERTY)) {
                    String filter = (String) evt.getNewValue();
                    setHostVmTreeFilter(filter);
                } else if (propertyName.equals(MainWindow.SHUTDOWN_PROPERTY)) {
                    stop();
                }
            }
            
        });
    }

}
