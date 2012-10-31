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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.HostInfo;
import com.redhat.thermostat.common.model.NetworkInterfaceInfo;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.internal.Activator;

public class HostOverviewViewPart extends ViewPart {

    private static final int FIRST_COLUMN_WIDTH = 150;
    private final String STR_UNKNOWN = "UNKNOWN";
    private PageBook pageBook;
    // VM page
    private Composite vmPage;
    private Label vmName;
    private Composite notConnectedPage;
    // Main compositie
    private ScrolledComposite mainScrollPage;
    private Label hostname;
    private Label procModel;
    private Label procCoreCount;
    private Label totalMemory;
    private TableViewer networkInterfaces;
    private Label osName;
    private Label osKernel;
    private ISelection oldSelection = null;

    // The listener we register with the selection service in order to listen
    // for
    // VmTreeView selection changes.
    private ISelectionListener listener = new ISelectionListener() {
        public void selectionChanged(IWorkbenchPart sourcepart,
                ISelection selection) {
            // only react upon hosts/vms tree changes. Then only if the selected
            // element
            // actually changed
            if (sourcepart instanceof HostsVmsTreeViewPart
                    && !selection.equals(oldSelection)) {
                oldSelection = selection;
                Ref ref = getRefFromSelection(selection);
                if (Activator.getDefault().isDbConnected()) {
                    if (ref != null) {
                        updateText(ref);
                        if (ref instanceof HostRef) {
                            showPage(mainScrollPage);
                        } else {
                            showPage(vmPage);
                        }
                    }
                } else {
                    showPage(notConnectedPage);
                }
            }
        }
    };

    private void showPage(final Control page) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (page instanceof ScrolledComposite) {
                    ((ScrolledComposite) page).getContent().pack();
                } else {
                    page.pack();
                }
                pageBook.showPage(page);
            }
        });
    }

    @Override
    public void createPartControl(Composite parent) {
        pageBook = new PageBook(parent, SWT.NONE);
        mainScrollPage = new ScrolledComposite(pageBook, SWT.NONE
                | SWT.V_SCROLL | SWT.H_SCROLL);
        Composite main = new Composite(mainScrollPage, SWT.NONE);
        mainScrollPage.setContent(main);
        vmPage = new Composite(pageBook, SWT.NONE);

        vmName = new Label(vmPage, SWT.NONE);
        vmPage.setLayout(new RowLayout());

        notConnectedPage = new Composite(pageBook, SWT.NONE);
        notConnectedPage.setLayout(new RowLayout());

        // ----------------------------------------
        // Not connected page
        // ----------------------------------------
        Label notConn = new Label(notConnectedPage, SWT.NONE);
        notConn.setText("Not connected to storage");

        // ----------------------------------------
        // Main overview page
        // ----------------------------------------
        main.setLayout(new GridLayout());

        // Basics
        Label lblBasics = new Label(main, SWT.NONE);
        lblBasics.setText("Basics"); // TODO: Externalize
        Font stdFont = lblBasics.getFont();
        Font boldFont = new Font(stdFont.getDevice(),
                stdFont.getFontData()[0].getName(),
                stdFont.getFontData()[0].getHeight(), SWT.BOLD);
        lblBasics.setFont(boldFont);
        Composite basicsComps = new Composite(main, SWT.NONE);
        GridLayout gridlayout = new GridLayout(2, false);
        basicsComps.setLayout(gridlayout);
        Label lblHostName = new Label(basicsComps, SWT.NONE);
        lblHostName.setText("Hostname");
        GridData hostNameGridData = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false);
        hostNameGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblHostName.setLayoutData(hostNameGridData);
        hostname = new Label(basicsComps, SWT.NONE);
        hostname.setText(STR_UNKNOWN);
        hostname.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

        // Hardware
        Label lblHardware = new Label(main, SWT.NONE);
        lblHardware.setText("Hardware");
        lblHardware.setFont(boldFont);
        Composite hardwareComps = new Composite(main, SWT.NONE);
        hardwareComps.setLayout(gridlayout);
        Label lblProcModel = new Label(hardwareComps, SWT.NONE);
        lblProcModel.setText("Processor Model");
        GridData procModelGridData = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false);
        procModelGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblProcModel.setLayoutData(procModelGridData);
        procModel = new Label(hardwareComps, SWT.NONE);
        procModel.setText(STR_UNKNOWN);
        procModel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
        Label lblProcCount = new Label(hardwareComps, SWT.NONE);
        lblProcCount.setText("Processor Count");
        GridData procCountGridData = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false);
        procCountGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblProcCount.setLayoutData(procCountGridData);
        procCoreCount = new Label(hardwareComps, SWT.NONE);
        procCoreCount.setText(STR_UNKNOWN);
        procCoreCount.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true,
                true));
        Label lblTotalMemory = new Label(hardwareComps, SWT.NONE);
        lblTotalMemory.setText("Total Memory");
        GridData totalMemGridData = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false);
        totalMemGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblTotalMemory.setLayoutData(totalMemGridData);
        lblTotalMemory.pack();
        totalMemory = new Label(hardwareComps, SWT.NONE);
        totalMemory.setText(STR_UNKNOWN);
        totalMemory
                .setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
        Label lblNetwork = new Label(hardwareComps, SWT.NONE);
        lblNetwork.setText("Network");
        GridData networkLayout = new GridData(SWT.RIGHT, SWT.TOP, true, true);
        networkLayout.widthHint = FIRST_COLUMN_WIDTH;
        lblNetwork.setLayoutData(networkLayout);
        networkInterfaces = new TableViewer(hardwareComps, SWT.BORDER);
        createNetworkTableViewer(networkInterfaces);

        // Software
        Label lblSoftware = new Label(main, SWT.NONE);
        lblSoftware.setText("Software");
        lblSoftware.setFont(boldFont);
        Composite softwareComps = new Composite(main, SWT.NONE);
        softwareComps.setLayout(gridlayout);
        Label lblOsName = new Label(softwareComps, SWT.NONE);
        lblOsName.setText("OS Name");
        GridData osNameGridData = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false);
        osNameGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblOsName.setLayoutData(osNameGridData);
        osName = new Label(softwareComps, SWT.NONE);
        osName.setText(STR_UNKNOWN);
        osName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
        Label lblKernel = new Label(softwareComps, SWT.NONE);
        lblKernel.setText("OS Kernel");
        GridData osKernelGridData = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false);
        osKernelGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblKernel.setLayoutData(osKernelGridData);
        osKernel = new Label(softwareComps, SWT.NONE);
        osKernel.setText(STR_UNKNOWN);
        osKernel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

        // Listen for VMtree changes
        getSite().getWorkbenchWindow().getSelectionService()
        .addSelectionListener(listener);
        if (Activator.getDefault().isDbConnected()) {
        	// Explicitly get the selected element from the VmsTreeViewPart
        	IViewPart part = getSite().getWorkbenchWindow().getActivePage().findView(ThermostatConstants.VIEW_ID_HOST_VM);
        	if (part != null && part instanceof HostsVmsTreeViewPart) {
        		ISelection selection = part.getSite().getSelectionProvider().getSelection();
        		Ref ref = getRefFromSelection(selection);
        		if (ref != null) {
        			updateText(ref);
        			if (ref instanceof HostRef) {
        				showPage(mainScrollPage);
        			} else {
        				showPage(vmPage);
        			}
        		} else {
        			// FIXME: probably want to show something else, e.g. select x in
        			// VM tree
        			showPage(notConnectedPage);
        		}
        	} else {
        		showPage(notConnectedPage);
        	}
        } else {
            showPage(notConnectedPage);
        }
    }

    @Override
    public void setFocus() {
        pageBook.setFocus();
    }

    private void updateText(Ref ref) {
        if (ref instanceof HostRef) {
            updateText((HostRef) ref);
        } else {
            updateText((VmRef) ref);
        }
    }

    private void updateText(final HostRef hostRef) {
        HostInfoDAO hostInfoDAO = OSGIUtils.getInstance().getService(
                HostInfoDAO.class);
        final HostInfo hostInfo = hostInfoDAO.getHostInfo(hostRef);
        final NetworkInterfaceInfoDAO networkInfoDAO = OSGIUtils.getInstance()
                .getService(NetworkInterfaceInfoDAO.class);
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                hostname.setText(hostRef.getHostName());
                osName.setText(hostInfo.getOsName());
                procModel.setText(hostInfo.getCpuModel());
                procCoreCount.setText(Integer.toString(hostInfo.getCpuCount()));
                osKernel.setText(hostInfo.getOsKernel());
                totalMemory.setText(Long.toString(hostInfo.getTotalMemory()));
                // set content for the network iface table
                networkInterfaces.setInput(networkInfoDAO.getNetworkInterfaces(
                        hostRef).toArray());
            }
        });
    }

    private void updateText(final VmRef vmRef) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                vmName.setText(vmRef.getName());
            }
        });
    }

    private Ref getRefFromSelection(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            // FIXME: hostsVms tree should only allow single selections
            for (Object item : ss.toArray()) {
                if (item instanceof Ref) {
                    return (Ref) item;
                }
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        // important: We need do unregister our listener when the view is
        // disposed
        getSite().getWorkbenchWindow().getSelectionService()
                .removeSelectionListener(listener);
        super.dispose();
    }

    private void createNetworkTableViewer(TableViewer viewer) {
        createColumns(viewer.getControl().getParent(), viewer);
        final Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        viewer.setContentProvider(new ArrayContentProvider());
        // create empty table
        viewer.setInput(new Object[0]);
    }

    // This will create the columns for the table
    private void createColumns(final Composite parent, final TableViewer viewer) {
        String[] titles = { "Interface", "IPv4 Address", "IPv6 Address" };
        int[] bounds = { 80, 150, 150 };

        // First column is iface name
        TableViewerColumn col = createTableViewerColumn(viewer, titles[0],
                bounds[0], 0);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                NetworkInterfaceInfo iface = (NetworkInterfaceInfo) element;
                return iface.getInterfaceName();
            }
        });

        // Second column is IPv4
        col = createTableViewerColumn(viewer, titles[1], bounds[1], 1);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                NetworkInterfaceInfo iface = (NetworkInterfaceInfo) element;
                return iface.getIp4Addr();
            }
        });

        // IPv6
        col = createTableViewerColumn(viewer, titles[2], bounds[2], 2);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                NetworkInterfaceInfo iface = (NetworkInterfaceInfo) element;
                return iface.getIp6Addr();
            }
        });
    }

    private TableViewerColumn createTableViewerColumn(TableViewer viewer,
            String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
                SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(false);
        return viewerColumn;
    }
}
