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

package com.redhat.thermostat.eclipse.internal.views;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
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
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.eclipse.internal.Activator;
import com.redhat.thermostat.eclipse.internal.ConnectionConfiguration;
import com.redhat.thermostat.eclipse.internal.controllers.ConnectDBAction;
import com.redhat.thermostat.eclipse.internal.controllers.ConnectionJobListener;
import com.redhat.thermostat.eclipse.internal.jobs.ConnectDbJob;
import com.redhat.thermostat.eclipse.internal.model.HostsVmsLabelProvider;
import com.redhat.thermostat.eclipse.internal.model.HostsVmsTreeContentProvider;
import com.redhat.thermostat.eclipse.internal.model.HostsVmsTreeRoot;

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

    public HostsVmsTreeViewPart() {
        // FIXME: Get these values from preferences
        ConnectionConfiguration configuration = new ConnectionConfiguration("dummyUser", "dummyPassword", "mongodb://127.0.0.1:27518");
        Job connectJob = new ConnectDbJob(
                "Connecting to Thermostat storage...", configuration);
        connectJob.setSystem(true);
        connectAction = new ConnectDBAction(connectJob);
        connectAction.setImageDescriptor(Activator
                .getImageDescriptor("icons/offline.png"));
        connectJob.addJobChangeListener(new ConnectionJobListener(connectAction, this));
    }
    
    public void showConnectionPage() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                pageBook.showPage(connectPage);
            }
        });
    }

    public void showHostVmsPage() {
        HostInfoDAO hostDAO = OSGIUtils.getInstance().getService(
                HostInfoDAO.class);
        VmInfoDAO vmsDAO = OSGIUtils.getInstance().getService(
                VmInfoDAO.class);
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
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(connectAction);

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
                connectAction.run();
            }
        });
        // Show appropriate page
        if (Activator.getDefault().isDbConnected()) {
            showHostVmsPage();
        } else {
            showConnectionPage();
        }
    }

    @Override
    public void setFocus() {
        pageBook.setFocus();
    }

}
