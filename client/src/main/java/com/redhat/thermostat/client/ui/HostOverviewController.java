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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.redhat.thermostat.client.AsyncUiFacade;
import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoConverter;
import com.redhat.thermostat.common.model.HostInfo;
import com.redhat.thermostat.common.model.NetworkInterfaceInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class HostOverviewController implements AsyncUiFacade {

    private static final Logger logger = LoggingUtils.getLogger(HostOverviewController.class);

    private final HostRef hostRef;
    private final HostInfoDAO hostInfoDAO;
    private final DBCollection networkInfoCollection;

    private final Timer backgroundUpdateTimer;

    private final HostOverviewView view;

    public HostOverviewController(HostRef ref, DB db) {
        hostInfoDAO = ApplicationContext.getInstance().getDAOFactory().getHostInfoDAO(ref);
        this.hostRef = ref;
        networkInfoCollection = db.getCollection("network-info");

        final Vector<String> networkTableColumnVector;
        networkTableColumnVector = new Vector<String>();
        networkTableColumnVector.add(localize(LocaleResources.NETWORK_INTERFACE_COLUMN));
        networkTableColumnVector.add(localize(LocaleResources.NETWORK_IPV4_COLUMN));
        networkTableColumnVector.add(localize(LocaleResources.NETWORK_IPV6_COLUMN));

        backgroundUpdateTimer = new Timer();
        view = createView();

        view.setNetworkTableColumns(networkTableColumnVector.toArray());
    }

    public HostOverviewView createView() {
        return new HostOverviewPanel();
    }

    private void doNetworkTableUpdateAsync() {
        new NetworkTableModelUpdater().execute();
    }

    private class NetworkTableModelUpdater extends SwingWorker<List<NetworkInterfaceInfo>, Void> {

        @Override
        protected List<NetworkInterfaceInfo> doInBackground() throws Exception {
            return getNetworkInfo();
        }

        private List<NetworkInterfaceInfo> getNetworkInfo() {
            List<NetworkInterfaceInfo> network = new ArrayList<NetworkInterfaceInfo>();
            DBCursor cursor = networkInfoCollection.find(new BasicDBObject("agent-id", hostRef.getAgentId()));
            while (cursor.hasNext()) {
                NetworkInterfaceInfo info = new NetworkInterfaceInfoConverter().fromDBObject(cursor.next());
                network.add(info);
            }

            return network;
        }

        @Override
        protected void done() {
            List<Object[]> data = new ArrayList<Object[]>();

            List<NetworkInterfaceInfo> networkInfo;
            try {
                networkInfo = get();
                for (NetworkInterfaceInfo info: networkInfo) {
                    String ifaceName = info.getInterfaceName();
                    String ipv4 = info.getIp4Addr();
                    String ipv6 = info.getIp6Addr();
                    data.add(new String[] { ifaceName, ipv4, ipv6 });
                }
                view.setNetworkTableData(data.toArray(new Object[0][0]));

            } catch (InterruptedException ie) {
                logger.log(Level.WARNING, "interrupted while updating network info", ie);
                // preserve interrupted flag
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                logger.log(Level.WARNING, "error updating network info", ee);
            }
        }
    }

    @Override
    public void start() {
        backgroundUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HostInfo hostInfo = hostInfoDAO.getHostInfo();
                view.setHostName(hostInfo.getHostname());
                view.setOsName(hostInfo.getOsName());
                view.setOsKernel(hostInfo.getOsKernel());
                view.setCpuModel(hostInfo.getCpuModel());
                view.setCpuCount(String.valueOf(hostInfo.getCpuCount()));
                view.setTotalMemory(String.valueOf(hostInfo.getTotalMemory()));

                doNetworkTableUpdateAsync();
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));

    }

    @Override
    public void stop() {
        backgroundUpdateTimer.cancel();
    }

    public Component getComponent() {
        return view.getUiComponent();
    }
}
