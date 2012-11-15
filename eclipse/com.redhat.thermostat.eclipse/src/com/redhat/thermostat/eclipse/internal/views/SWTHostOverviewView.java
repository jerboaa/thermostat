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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import com.redhat.thermostat.client.core.views.HostOverviewView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.ThermostatConstants;

public class SWTHostOverviewView extends HostOverviewView implements
        SWTComponent {
    private static final String CLASS_NAME = SWTHostOverviewView.class.getSimpleName();
    public static final String TEST_ID_HOSTNAME = CLASS_NAME + ".hostname";
    public static final String TEST_ID_PROC_MODEL = CLASS_NAME + ".procModel";
    public static final String TEST_ID_PROC_CORE_COUNT = CLASS_NAME + ".procCoreCount";
    public static final String TEST_ID_TOTAL_MEMORY = CLASS_NAME + ".totalMemory";
    public static final String TEST_ID_NETWORK_INTERFACES = CLASS_NAME + ".networkInterfaces";
    public static final String TEST_ID_OS_NAME = CLASS_NAME + ".osName";
    public static final String TEST_ID_OS_KERNEL = CLASS_NAME + ".osKernel";
    
    private static final Translate<LocaleResources> translator = LocaleResources
            .createLocalizer();
    private static final int FIRST_COLUMN_WIDTH = 150;
    private static final String STR_UNKNOWN = "UNKNOWN";
    private static final int NUM_TABLE_ROWS = 5;
    private static int[] TABLE_COLUMN_WIDTHS = { 80, 150, 300 };

    private Label hostname;
    private Label procModel;
    private Label procCoreCount;
    private Label totalMemory;
    private TableViewer networkInterfaces;
    private Label osName;
    private Label osKernel;

    public SWTHostOverviewView(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout());
        top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Basics
        Label lblBasics = new Label(top, SWT.NONE);
        lblBasics.setText(translator
                .localize(LocaleResources.HOST_OVERVIEW_SECTION_BASICS));
        Font stdFont = lblBasics.getFont();
        Font boldFont = new Font(stdFont.getDevice(),
                stdFont.getFontData()[0].getName(),
                stdFont.getFontData()[0].getHeight(), SWT.BOLD);
        lblBasics.setFont(boldFont);
        Composite basicsComps = new Composite(top, SWT.NONE);
        GridLayout gridlayout = new GridLayout(2, false);
        basicsComps.setLayout(gridlayout);
        basicsComps.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                false));
        Label lblHostName = new Label(basicsComps, SWT.NONE);
        lblHostName.setText(translator
                .localize(LocaleResources.HOST_INFO_HOSTNAME));
        GridData hostNameGridData = new GridData(SWT.FILL, SWT.CENTER, false,
                false);
        hostNameGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblHostName.setLayoutData(hostNameGridData);
        hostname = new Label(basicsComps, SWT.NONE);
        hostname.setData(ThermostatConstants.TEST_TAG, TEST_ID_HOSTNAME);
        hostname.setText(STR_UNKNOWN);
        hostname.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Hardware
        Label lblHardware = new Label(top, SWT.NONE);
        lblHardware.setText(translator
                .localize(LocaleResources.HOST_OVERVIEW_SECTION_HARDWARE));
        lblHardware.setFont(boldFont);
        Composite hardwareComps = new Composite(top, SWT.NONE);
        hardwareComps.setLayout(gridlayout);
        hardwareComps.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                false));
        Label lblProcModel = new Label(hardwareComps, SWT.NONE);
        lblProcModel.setText(translator
                .localize(LocaleResources.HOST_INFO_CPU_MODEL));
        GridData procModelGridData = new GridData(SWT.FILL, SWT.CENTER, false,
                false);
        procModelGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblProcModel.setLayoutData(procModelGridData);
        procModel = new Label(hardwareComps, SWT.NONE);
        procModel.setData(ThermostatConstants.TEST_TAG, TEST_ID_PROC_MODEL);
        procModel.setText(STR_UNKNOWN);
        procModel
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Label lblProcCount = new Label(hardwareComps, SWT.NONE);
        lblProcCount.setText(translator
                .localize(LocaleResources.HOST_INFO_CPU_COUNT));
        GridData procCountGridData = new GridData(SWT.FILL, SWT.CENTER, false,
                false);
        procCountGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblProcCount.setLayoutData(procCountGridData);
        procCoreCount = new Label(hardwareComps, SWT.NONE);
        procCoreCount.setData(ThermostatConstants.TEST_TAG, TEST_ID_PROC_CORE_COUNT);
        procCoreCount.setText(STR_UNKNOWN);
        procCoreCount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false));
        Label lblTotalMemory = new Label(hardwareComps, SWT.NONE);
        lblTotalMemory.setText(translator
                .localize(LocaleResources.HOST_INFO_MEMORY_TOTAL));
        GridData totalMemGridData = new GridData(SWT.FILL, SWT.CENTER, false,
                false);
        totalMemGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblTotalMemory.setLayoutData(totalMemGridData);
        totalMemory = new Label(hardwareComps, SWT.NONE);
        totalMemory.setData(ThermostatConstants.TEST_TAG, TEST_ID_TOTAL_MEMORY);
        totalMemory.setText(STR_UNKNOWN);
        totalMemory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false));
        Label lblNetwork = new Label(hardwareComps, SWT.NONE);
        lblNetwork.setText(translator
                .localize(LocaleResources.HOST_INFO_NETWORK));
        GridData networkLayout = new GridData(SWT.FILL, SWT.TOP, false, false);
        networkLayout.widthHint = FIRST_COLUMN_WIDTH;
        lblNetwork.setLayoutData(networkLayout);
        networkInterfaces = new TableViewer(hardwareComps, SWT.BORDER);
        networkInterfaces.getTable().setData(ThermostatConstants.TEST_TAG, TEST_ID_NETWORK_INTERFACES);
        createNetworkTableViewer();

        // Software
        Label lblSoftware = new Label(top, SWT.NONE);
        lblSoftware.setText(translator
                .localize(LocaleResources.HOST_OVERVIEW_SECTION_SOFTWARE));
        lblSoftware.setFont(boldFont);
        Composite softwareComps = new Composite(top, SWT.NONE);
        softwareComps.setLayout(gridlayout);
        softwareComps.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                false));
        Label lblOsName = new Label(softwareComps, SWT.NONE);
        lblOsName.setText(translator
                .localize(LocaleResources.HOST_INFO_OS_NAME));
        GridData osNameGridData = new GridData(SWT.FILL, SWT.CENTER, false,
                false);
        osNameGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblOsName.setLayoutData(osNameGridData);
        osName = new Label(softwareComps, SWT.NONE);
        osName.setData(ThermostatConstants.TEST_TAG, TEST_ID_OS_NAME);
        osName.setText(STR_UNKNOWN);
        osName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Label lblKernel = new Label(softwareComps, SWT.NONE);
        lblKernel.setText(translator
                .localize(LocaleResources.HOST_INFO_OS_KERNEL));
        GridData osKernelGridData = new GridData(SWT.FILL, SWT.CENTER, false,
                false);
        osKernelGridData.widthHint = FIRST_COLUMN_WIDTH;
        lblKernel.setLayoutData(osKernelGridData);
        osKernel = new Label(softwareComps, SWT.NONE);
        osKernel.setData(ThermostatConstants.TEST_TAG, TEST_ID_OS_KERNEL);
        osKernel.setText(STR_UNKNOWN);
        osKernel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    @Override
    public void setHostName(final String newHostName) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!hostname.isDisposed()) {
                    hostname.setText(newHostName);
                }
            }
        });
    }

    @Override
    public void setCpuModel(final String newCpuModel) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!procModel.isDisposed()) {
                    procModel.setText(newCpuModel);
                }
            }
        });
    }

    @Override
    public void setCpuCount(final String newCpuCount) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!procCoreCount.isDisposed()) {
                    procCoreCount.setText(newCpuCount);
                }
            }
        });
    }

    @Override
    public void setTotalMemory(final String newTotalMemory) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!totalMemory.isDisposed()) {
                    totalMemory.setText(newTotalMemory);
                }
            }
        });
    }

    @Override
    public void setOsName(final String newOsName) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!osName.isDisposed()) {
                    osName.setText(newOsName);
                }
            }
        });
    }

    @Override
    public void setOsKernel(final String newOsKernel) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!osKernel.isDisposed()) {
                    osKernel.setText(newOsKernel);
                }
            }
        });
    }

    @Override
    public void setNetworkTableColumns(final Object[] columns) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!networkInterfaces.getTable().isDisposed()) {
                    for (int i = 0; i < columns.length; i++) {
                        Object column = columns[i];
                        createTableViewerColumn(column.toString(),
                                TABLE_COLUMN_WIDTHS[i]);
                        networkInterfaces.getTable().getParent().layout();
                    }
                }
            }
        });
    }

    @Override
    public void setInitialNetworkTableData(final Object[][] table) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!networkInterfaces.getTable().isDisposed()) {
                    networkInterfaces.setInput(table);
                }
            }
        });
    }

    @Override
    public void updateNetworkTableData(final int row, final int column,
            final String data) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!networkInterfaces.getTable().isDisposed()) {
                    Object[][] input = (Object[][]) networkInterfaces
                            .getInput();
                    input[row][column] = data;
                    networkInterfaces.refresh();
                }
            }
        });
    }

    private void createNetworkTableViewer() {
        final Table table = networkInterfaces.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayout(new GridLayout());
        GridData tableGridData = new GridData();
        int height = NUM_TABLE_ROWS
                * networkInterfaces.getTable().getItemHeight();
        tableGridData.heightHint = height;
        table.setLayoutData(tableGridData);

        networkInterfaces.setContentProvider(new ArrayContentProvider());

        // create empty table
        networkInterfaces.setInput(new Object[0]);
    }

    private TableViewerColumn createTableViewerColumn(String title, int bound) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(
                networkInterfaces, SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(false);

        viewerColumn.setLabelProvider(new CellLabelProvider() {

            @Override
            public void update(ViewerCell cell) {
                int idx = cell.getColumnIndex();
                Object[] array = (Object[]) cell.getElement();
                Object element = array[idx];
                if (element != null) {
                    cell.setText(element.toString());
                }
            }
        });
        return viewerColumn;
    }

    @Override
    public void show() {
        notifier.fireAction(Action.VISIBLE);
    }

    @Override
    public void hide() {
        notifier.fireAction(Action.HIDDEN);
    }

}
