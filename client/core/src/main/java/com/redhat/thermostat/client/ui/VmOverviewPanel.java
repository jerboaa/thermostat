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

import com.redhat.thermostat.client.internal.ChangeableText;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.BasicView;

public class VmOverviewPanel extends VmOverviewView implements SwingComponent {
    
    private JPanel visiblePanel;

    private final ChangeableText pid = new ChangeableText("");
    private final ChangeableText startTimeStamp = new ChangeableText("");
    private final ChangeableText stopTimeStamp = new ChangeableText("");
    private final ChangeableText mainClass = new ChangeableText("");
    private final ChangeableText javaCommandLine = new ChangeableText("");
    private final ChangeableText javaHome = new ChangeableText("");
    private final ChangeableText javaVersion = new ChangeableText("");
    private final ChangeableText vmNameAndVersion = new ChangeableText("");
    private final ChangeableText vmArguments = new ChangeableText("");

    public VmOverviewPanel() {
        super();
        initializePanel();
        visiblePanel.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(Component component) {
                notifier.fireAction(Action.HIDDEN);
            }
        });
    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public void setVmPid(String pid) {
        this.pid.setText(pid);
    }

    @Override
    public void setVmStartTimeStamp(String timeStamp) {
        this.startTimeStamp.setText(timeStamp);
    }

    @Override
    public void setVmStopTimeStamp(String timeStamp) {
        this.stopTimeStamp.setText(timeStamp);
    }

    @Override
    public void setMainClass(String mainClass) {
        this.mainClass.setText(mainClass);
    }

    @Override
    public void setJavaCommandLine(String javaCommandLine) {
        this.javaCommandLine.setText(javaCommandLine);
    }

    @Override
    public void setJavaHome(String javaHome) {
        this.javaHome.setText(javaHome);

    }

    @Override
    public void setJavaVersion(String javaVersion) {
        this.javaVersion.setText(javaVersion);
    }

    @Override
    public void setVmNameAndVersion(String vmNameAndVersion) {
        this.vmNameAndVersion.setText(vmNameAndVersion);
    }

    @Override
    public void setVmArguments(String vmArguments) {
        this.vmArguments.setText(vmArguments);
    }

    @Override
    public void setVmInfo(String string) {
        // no-op
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel = new JPanel();
        visiblePanel.setBorder(Components.smallBorder());
        visiblePanel.setLayout(new BorderLayout());

        TableEntry entry;
        List<Section> allSections = new ArrayList<Section>();

        Section processSection = new Section(localize(LocaleResources.VM_INFO_SECTION_PROCESS));
        allSections.add(processSection);

        entry = new TableEntry(localize(LocaleResources.VM_INFO_PROCESS_ID), pid);
        processSection.add(entry);
        entry = new TableEntry(localize(LocaleResources.VM_INFO_START_TIME), startTimeStamp);
        processSection.add(entry);
        entry = new TableEntry(localize(LocaleResources.VM_INFO_STOP_TIME), stopTimeStamp);
        processSection.add(entry);

        Section javaSection = new Section(localize(LocaleResources.VM_INFO_SECTION_JAVA));
        allSections.add(javaSection);

        entry = new TableEntry(localize(LocaleResources.VM_INFO_MAIN_CLASS), mainClass);
        javaSection.add(entry);
        entry = new TableEntry(localize(LocaleResources.VM_INFO_COMMAND_LINE), javaCommandLine);
        javaSection.add(entry);
        entry = new TableEntry(localize(LocaleResources.VM_INFO_JAVA_VERSION), javaVersion);
        javaSection.add(entry);
        entry = new TableEntry(localize(LocaleResources.VM_INFO_VM), vmNameAndVersion);
        javaSection.add(entry);
        entry = new TableEntry(localize(LocaleResources.VM_INFO_VM_ARGUMENTS), vmArguments);
        javaSection.add(entry);

        SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        visiblePanel.add(table, BorderLayout.PAGE_START);
    }

    @Override
    public BasicView getView() {
        return this;
    }

}
