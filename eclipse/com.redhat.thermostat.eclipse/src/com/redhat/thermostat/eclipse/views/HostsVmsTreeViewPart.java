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

package com.redhat.thermostat.eclipse.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.redhat.thermostat.common.DefaultHostsVMsLoader;
import com.redhat.thermostat.common.HostsVMsLoader;
import com.redhat.thermostat.common.ThreadPoolTimerFactory;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;
import com.redhat.thermostat.common.storage.ConnectionException;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.eclipse.Activator;
import com.redhat.thermostat.eclipse.ConnectionConfiguration;
import com.redhat.thermostat.eclipse.LoggerFacility;
import com.redhat.thermostat.eclipse.model.HostsVmsLabelProvider;
import com.redhat.thermostat.eclipse.model.HostsVmsTreeContentProvider;
import com.redhat.thermostat.eclipse.model.HostsVmsTreeRoot;

/**
 * 
 * The main class for the VM tree view of the Thermostat Eclipse client.
 * 
 */
public class HostsVmsTreeViewPart extends ViewPart {

    private Action connectAction;
    // Hosts and VMs viewer
    private TreeViewer treeViewer;
    // viewer for the connect viewing.
    private Composite connectPage;
    // Container for tree and connect
    private PageBook pageBook;

    private ConnectionConfiguration configuration;

    private void showConnectionPage() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                pageBook.showPage(connectPage);
            }
        });
    }

    private void showHostVmsPage() {
        HostInfoDAO hostDAO = ApplicationContext.getInstance().getDAOFactory()
                .getHostInfoDAO();
        VmInfoDAO vmsDAO = ApplicationContext.getInstance().getDAOFactory()
                .getVmInfoDAO();
        final HostsVMsLoader loader = new DefaultHostsVMsLoader(hostDAO,
                vmsDAO, false);

        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                treeViewer.setContentProvider(new HostsVmsTreeContentProvider(
                        loader));
                treeViewer.setLabelProvider(new HostsVmsLabelProvider());
                treeViewer.setUseHashlookup(true);
                treeViewer.setInput(new HostsVmsTreeRoot());
                pageBook.showPage(treeViewer.getControl());
            }

        });
    }

    @Override
    public void createPartControl(final Composite parent) {
        connectAction = new Action("Connect to storage...") {
            public void run() {
                Job connectJob = new ConnectJob(
                        "Connecting to Thermostat storage...");
                connectJob.setSystem(true);
                connectJob.addJobChangeListener(new ConnectionJobListener());
                connectJob.schedule();
            }
        };
        connectAction.setImageDescriptor(Activator
                .getImageDescriptor("icons/offline.png"));
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(connectAction);

        configuration = new ConnectionConfiguration("mongodb://127.0.0.1:27518");
        pageBook = new PageBook(parent, SWT.NONE);

        // Prepare Hosts/VMs tree
        treeViewer = new TreeViewer(pageBook, SWT.NONE);
        // register the tree as selection provider
        getSite().setSelectionProvider(treeViewer);

        // Prepare connect page
        RowLayout layout = new RowLayout();
        layout.wrap = true;
        connectPage = new Composite(pageBook, SWT.NONE);
        connectPage.setLayout(layout);
        Label test = new Label(connectPage, SWT.NONE);
        test.setText("Not connected... ");
        Link link = new Link(connectPage, SWT.NONE);
        // FIXME: Externalize
        link.setText("<a>Connect</a>");
        link.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                // implement connect
                Job connectJob = new ConnectJob(
                        "Connecting to Thermostat storage...");
                connectJob.setSystem(true);
                connectJob.addJobChangeListener(new ConnectionJobListener());
                connectJob.schedule();
            }
        });
        // Show appropriate page
        boolean connected = Activator.getDefault().isConnected();
        if (connected) {
            showHostVmsPage();
        } else {
            showConnectionPage();
        }
    }

    @Override
    public void setFocus() {
        pageBook.setFocus();
    }

    /*
     * Mongo connection method
     */
    private boolean connectToBackEnd() throws InvalidConfigurationException {
        StorageProvider connProv = new MongoStorageProvider(configuration);
        DAOFactory daoFactory = new MongoDAOFactory(connProv);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        TimerFactory timerFactory = new ThreadPoolTimerFactory(1);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        Connection connection = daoFactory.getConnection();
        ConnectionListener connectionListener = new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case DISCONNECTED:
                    LoggerFacility.getInstance().log(IStatus.WARNING,
                            "Unexpected disconnect event.");
                    break;
                case CONNECTING:
                    LoggerFacility.getInstance().log(IStatus.INFO,
                            "Connecting to storage.");
                    break;
                case CONNECTED:
                    LoggerFacility.getInstance().log(IStatus.INFO,
                            "Connected to storage.");
                    Activator.getDefault().setConnected(true);
                    break;
                case FAILED_TO_CONNECT:
                    LoggerFacility.getInstance().log(IStatus.WARNING,
                            "Could not connect to storage.");
                default:
                    LoggerFacility.getInstance().log(IStatus.WARNING,
                            "Unfamiliar ConnectionStatus value");
                }
            }
        };
        connection.addListener(connectionListener);
        try {
            LoggerFacility.getInstance().log(IStatus.INFO,
                    "Connecting to storage...");
            connection.connect();
            return true;
        } catch (final ConnectionException e) {
            LoggerFacility.getInstance().log(IStatus.ERROR,
                    e.getCause().getMessage(), e.getCause());
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    // FIXME: Show a nicer error message
                    MessageDialog.openError(null, "Connection Problem", e
                            .getCause().getMessage());
                }

            });
            return false;
        }
    }

    private class ConnectJob extends Job {

        public ConnectJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(
                    "Connecting to " + configuration.getDBConnectionString(),
                    IProgressMonitor.UNKNOWN);
            try {
                if (connectToBackEnd()) {
                    return Status.OK_STATUS;
                }
            } catch (InvalidConfigurationException e) {
                // FIXME: do something more reasonable
            }
            return Status.CANCEL_STATUS;
        }

    }

    private class ConnectionJobListener extends JobChangeAdapter {

        @Override
        public void done(IJobChangeEvent event) {
            IStatus result = event.getResult();
            if (result.isOK() && result.getCode() != IStatus.CANCEL) {
                showHostVmsPage();
                connectAction.setImageDescriptor(Activator
                        .getImageDescriptor("icons/online.png"));
                connectAction.setEnabled(!Activator.getDefault().isConnected());
                connectAction.setToolTipText("Online");
            }
        }

    }

}
