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

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.ui.AboutDialog;
import com.redhat.thermostat.client.ui.AgentConfigurationController;
import com.redhat.thermostat.client.ui.AgentConfigurationModel;
import com.redhat.thermostat.client.ui.AgentConfigurationView;
import com.redhat.thermostat.client.ui.ClientConfigurationController;
import com.redhat.thermostat.client.ui.ClientConfigurationView;
import com.redhat.thermostat.client.ui.HostPanel;
import com.redhat.thermostat.client.ui.SummaryPanel;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class MainWindowControllerImpl implements MainWindowController {

    private Timer backgroundUpdater;

    private MainView view;

    private String filter;

    private final HostInfoDAO hostsDAO;
    private final VmInfoDAO vmsDAO;

    private ApplicationInfo appInfo;

    private UiFacadeFactory facadeFactory;

    private boolean showHistory;

    private VmInformationControllerProvider vmInfoControllerProvider;

    public MainWindowControllerImpl(UiFacadeFactory facadeFactory, MainView view) {
        this.facadeFactory = facadeFactory;

        ApplicationContext ctx = ApplicationContext.getInstance();
        DAOFactory daoFactory = ctx.getDAOFactory();
        hostsDAO = daoFactory.getHostInfoDAO();
        vmsDAO = daoFactory.getVmInfoDAO();

        initView(view);

        vmInfoControllerProvider = new VmInformationControllerProvider();

        appInfo = new ApplicationInfo();
        view.setWindowTitle(appInfo.getName());
        initializeTimer();

        updateView();
    }

    private class HostsVMsLoaderImpl implements HostsVMsLoader {

        @Override
        public Collection<HostRef> getHosts() {
            if (showHistory) {
                return hostsDAO.getHosts();
            } else {
                return hostsDAO.getAliveHosts();
            }
        }

        @Override
        public Collection<VmRef> getVMs(HostRef host) {
            return vmsDAO.getVMs(host);
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
        backgroundUpdater.setDelay(3);
        backgroundUpdater.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdater.setSchedulingType(SchedulingType.FIXED_RATE);
    }

    private void startBackgroundUpdates() {
        backgroundUpdater.start();
    }

    public void stopBackgroundUpdates() {
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
                case VISIBLE:
                    startBackgroundUpdates();
                    break;
                case HIDDEN:
                    stopBackgroundUpdates();
                    break;
                case HOST_VM_SELECTION_CHANGED:
                    updateView();
                    break;
                case HOST_VM_TREE_FILTER:
                    String filter = view.getHostVmTreeFilter();
                    setHostVmTreeFilter(filter);
                    break;
                case SHOW_AGENT_CONFIG:
                    showAgentConfiguration();
                    break;
                case SHOW_CLIENT_CONFIG:
                    showConfigureClientPreferences();
                    break;
                case SWITCH_HISTORY_MODE:
                    switchHistoryMode();
                    break;
                case SHOW_ABOUT_DIALOG:
                    showAboutDialog();
                    break;
                case SHUTDOWN:
                    view.hideMainWindow();
                    ApplicationContext.getInstance().getTimerFactory().shutdown();
                    break;
                default:
                    throw new IllegalStateException("unhandled action");
                }
            }
        });
    }

    @Override
    public void showMainMainWindow() {
        view.showMainWindow();
    }

    private void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(appInfo);
        aboutDialog.setModal(true);
        aboutDialog.pack();
        aboutDialog.setVisible(true);
    }

    private void showAgentConfiguration() {
        AgentConfigurationSource agentPrefs = new AgentConfigurationSource();
        AgentConfigurationModel model = new AgentConfigurationModel(agentPrefs);
        AgentConfigurationView view = ApplicationContext.getInstance().getViewFactory().getView(AgentConfigurationView.class);
        AgentConfigurationController controller = new AgentConfigurationController(model, view);
        controller.showView();
    }

    private void showConfigureClientPreferences() {
        ClientPreferences prefs = new ClientPreferences();
        ClientConfigurationView view = ApplicationContext.getInstance().getViewFactory().getView(ClientConfigurationView.class);
        ClientConfigurationController controller = new ClientConfigurationController(prefs, view);
        controller.showDialog();
    }

    private void switchHistoryMode() {
        showHistory = !showHistory;
        doUpdateTreeAsync();
    }

    private void updateView() {
        // this is quite an ugly method. there must be a cleaner way to do this
        Ref ref = view.getSelectedHostOrVm();

        if (ref == null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    view.setSubView(new SummaryPanel(facadeFactory.getSummaryPanel()));

                }
            });
        } else if (ref instanceof HostRef) {
            HostRef hostRef = (HostRef) ref;
            view.setSubView(new HostPanel(facadeFactory.getHostPanel(hostRef)));
        } else if (ref instanceof VmRef) {
            VmRef vmRef = (VmRef) ref;
            VmInformationController vmInformation =
                    vmInfoControllerProvider.getVmInfoController(vmRef);
            view.setSubView(vmInformation.getComponent());
        } else {
            throw new IllegalArgumentException("unknown type of ref");
        }
    }

    private class VmInformationControllerProvider {
        private VmInformationController lastSelectedVM;

        VmInformationController getVmInfoController(VmRef vmRef) {
            int id = 0;
            if (lastSelectedVM != null) {
                id = lastSelectedVM.getSelectedChildID();
            }
            lastSelectedVM = facadeFactory.getVmController(vmRef);
            lastSelectedVM.selectChildID(id);

            return lastSelectedVM;
        }
    }

}
