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

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.HostRefDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.dao.VmRefDAO;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class MainWindowControllerImpl implements MainWindowController {

    private Timer backgroundUpdater;

    private MainView view;

    private String filter;

    private HostRefDAO hostRefDAO;
    private VmRefDAO vmRefDAO;

    public MainWindowControllerImpl(MainView view) {

        ApplicationContext ctx = ApplicationContext.getInstance();
        DAOFactory daoFactory = ctx.getDAOFactory();
        hostRefDAO = daoFactory.getHostRefDAO();
        vmRefDAO = daoFactory.getVmRefDAO();

        initView(view);
        initializeTimer();
        start();
    }

    private class HostsVMsLoaderImpl implements HostsVMsLoader {

        @Override
        public Collection<HostRef> getHosts() {
            return hostRefDAO.getHosts();
        }

        @Override
        public Collection<VmRef> getVMs(HostRef host) {
            return vmRefDAO.getVMs(host);
        }
        
    }

    private void initializeTimer() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        backgroundUpdater = ctx.getTimerFactory().createTimer();
        backgroundUpdater.setAction(new Runnable() {
            @Override
            public void run() {
                doUpdateTreeAsync();
            }
        });
        backgroundUpdater.setInitialDelay(0);
        backgroundUpdater.setDelay(10);
        backgroundUpdater.setTimeUnit(TimeUnit.SECONDS);
    }

    @Override
    public void start() {
        backgroundUpdater.start();
    }

    @Override
    public void stop() {
        backgroundUpdater.stop();
    }

    @Override
    public void setHostVmTreeFilter(String filter) {
        this.filter = filter;
        doUpdateTreeAsync();
    }

    public void doUpdateTreeAsync() {
        HostsVMsLoader loader = new HostsVMsLoaderImpl();
        view.updateTree(filter, loader);
    }

    private void initView(MainView mainView) {
        this.view = mainView;
        mainView.addActionListener(new ActionListener<MainView.Action>() {

            @Override
            public void actionPerformed(ActionEvent<MainView.Action> evt) {
                MainView.Action action = evt.getActionId();
                switch (action) {
                case HOST_VM_TREE_FILTER:
                    String filter = view.getHostVmTreeFilter();
                    setHostVmTreeFilter(filter);
                    break;
                case SHUTDOWN:
                    stop();
                    break;
                default:
                    assert false;
                }
            }
            
        });
    }

    @Override
    public void showMainMainWindow() {
        view.showMainWindow();
    }

}
