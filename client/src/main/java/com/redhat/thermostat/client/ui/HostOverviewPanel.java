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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.ChangeableText;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.SimpleTable.Key;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;
import com.redhat.thermostat.client.ui.SimpleTable.Value;

public class HostOverviewPanel implements HostOverviewView {

    private final ChangeableText hostname;
    private final ChangeableText cpuModel;
    private final ChangeableText cpuCount;
    private final ChangeableText totalMemory;
    private final ChangeableText osName;
    private final ChangeableText osKernel;

    private final DefaultTableModel networkTableModel;
    private Object[] networkTableColumns;
    private Object[][] networkTableData;

    public HostOverviewPanel() {
        hostname = new ChangeableText("");
        cpuModel = new ChangeableText("");
        cpuCount = new ChangeableText("");
        totalMemory = new ChangeableText("");
        osName = new ChangeableText("");
        osKernel = new ChangeableText("");

        networkTableModel = new DefaultTableModel();
    }

    @Override
    public void setHostName(String newHostName) {
        hostname.setText(newHostName);
    }

    @Override
    public void setCpuModel(String newCpuModel) {
        cpuModel.setText(newCpuModel);
    }

    @Override
    public void setCpuCount(String newCpuCount) {
        cpuCount.setText(newCpuCount);
    }

    @Override
    public void setTotalMemory(String newTotalMemory) {
        totalMemory.setText(newTotalMemory);
    }

    @Override
    public void setOsName(String newOsName) {
        osName.setText(newOsName);
    }

    @Override
    public void setOsKernel(String newOsKernel) {
        osKernel.setText(newOsKernel);
    }

    @Override
    public void setNetworkTableColumns(Object[] columns) {
        this.networkTableColumns = columns;
        networkTableModel.setDataVector(this.networkTableData, this.networkTableColumns);
    }

    @Override
    public void setNetworkTableData(Object[][] data) {
        this.networkTableData = data;
        networkTableModel.setDataVector(this.networkTableData, this.networkTableColumns);
    }

    @Override
    public Component getUiComponent() {

        TableEntry entry;
        List<Section> allSections = new ArrayList<Section>();

        Section basics = new Section(localize(LocaleResources.HOST_OVERVIEW_SECTION_BASICS));
        allSections.add(basics);

        entry = new TableEntry(localize(LocaleResources.HOST_INFO_HOSTNAME), hostname);
        basics.add(entry);

        Section hardware = new Section(localize(LocaleResources.HOST_OVERVIEW_SECTION_HARDWARE));
        allSections.add(hardware);

        entry = new TableEntry(localize(LocaleResources.HOST_INFO_CPU_MODEL), cpuModel);
        hardware.add(entry);
        entry = new TableEntry(localize(LocaleResources.HOST_INFO_CPU_COUNT), cpuCount);
        hardware.add(entry);
        entry = new TableEntry(localize(LocaleResources.HOST_INFO_MEMORY_TOTAL), totalMemory);
        hardware.add(entry);

        JTable networkTable = new JTable(networkTableModel);

        JPanel networkPanel = new JPanel(new BorderLayout());
        networkPanel.add(networkTable.getTableHeader(), BorderLayout.PAGE_START);
        networkPanel.add(networkTable, BorderLayout.CENTER);

        Key key = new Key(localize(LocaleResources.HOST_INFO_NETWORK));
        hardware.add(new TableEntry(key, new Value(networkPanel)));

        Section software = new Section(localize(LocaleResources.HOST_OVERVIEW_SECTION_SOFTWARE));
        allSections.add(software);

        entry = new TableEntry(localize(LocaleResources.HOST_INFO_OS_NAME), osName);
        software.add(entry);
        entry = new TableEntry(localize(LocaleResources.HOST_INFO_OS_KERNEL), osKernel);
        software.add(entry);

        SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        return table;
    }

}
