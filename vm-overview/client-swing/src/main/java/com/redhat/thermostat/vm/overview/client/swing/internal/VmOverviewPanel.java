/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.overview.client.swing.internal;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.overview.client.core.VmOverviewView;
import com.redhat.thermostat.vm.overview.client.locale.LocaleResources;

public class VmOverviewPanel extends VmOverviewView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private HeaderPanel visiblePanel;
    private JScrollPane container;

    private final ValueField pid = new ValueField("");
    private final ValueField startTimeStamp = new ValueField("");
    private final ValueField stopTimeStamp = new ValueField("");
    private final ValueField mainClass = new ValueField("");
    private final ValueField javaCommandLine = new ValueField("");
    private final ValueField javaHome = new ValueField("");
    private final ValueField javaVersion = new ValueField("");
    private final ValueField vmNameAndVersion = new ValueField("");
    private final ValueField vmArguments = new ValueField("");
    private final ValueField user = new ValueField("");

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
    public void setVmPid(final String newPid) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pid.setText(newPid);
            }
        });
    }

    @Override
    public void setVmStartTimeStamp(final String newTimeStamp) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startTimeStamp.setText(newTimeStamp);
            }
        });
    }

    @Override
    public void setVmStopTimeStamp(final String newTimeStamp) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stopTimeStamp.setText(newTimeStamp);
            }
        });
    }

    @Override
    public void setMainClass(final String newMainClass) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainClass.setText(newMainClass);
            }
        });
    }

    @Override
    public void setJavaCommandLine(final String newJavaCommandLine) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                javaCommandLine.setText(newJavaCommandLine);
            }
        });
    }

    @Override
    public void setJavaHome(final String newJavaHome) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                javaHome.setText(newJavaHome);
            }
        });

    }

    @Override
    public void setJavaVersion(final String newJavaVersion) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                javaVersion.setText(newJavaVersion);
            }
        });
    }

    @Override
    public void setVmNameAndVersion(final String newVmNameAndVersion) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                vmNameAndVersion.setText(newVmNameAndVersion);
            }
        });
    }

    @Override
    public void setVmArguments(final String newVmArguments) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                vmArguments.setText(newVmArguments);
            }
        });
    }

    @Override
    public void setUserID(final String userID) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                user.setText(userID);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel = new HeaderPanel();

        visiblePanel.setHeader(translator.localize(LocaleResources.VM_INFO_TITLE));

        SectionHeader processSection = new SectionHeader(translator.localize(LocaleResources.VM_INFO_SECTION_PROCESS));
        LabelField pidLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_PROCESS_ID));
        LabelField startTimeLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_START_TIME));
        LabelField stopTimeLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_STOP_TIME));
        LabelField userLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_USER));

        SectionHeader javaSection = new SectionHeader(translator.localize(LocaleResources.VM_INFO_SECTION_JAVA));

        LabelField mainClassLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_MAIN_CLASS));
        LabelField javaCommandLineLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_COMMAND_LINE));
        LabelField javaVersionLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_JAVA_VERSION));
        LabelField vmNameAndVersionLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_VM));
        LabelField vmArgumentsLabel = new LabelField(translator.localize(LocaleResources.VM_INFO_VM_ARGUMENTS));

        JPanel table = new JPanel();
        table.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));
        GroupLayout gl = new GroupLayout(table);
        table.setLayout(gl);

        gl.setHorizontalGroup(gl.createParallelGroup()
                .addComponent(processSection)
                .addComponent(javaSection)
                .addGroup(gl.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(gl.createParallelGroup(Alignment.TRAILING)
                                .addComponent(pidLabel)
                                .addComponent(startTimeLabel)
                                .addComponent(stopTimeLabel)
                                .addComponent(userLabel)
                                .addComponent(mainClassLabel)
                                .addComponent(javaCommandLineLabel)
                                .addComponent(javaVersionLabel)
                                .addComponent(vmNameAndVersionLabel)
                                .addComponent(vmArgumentsLabel))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl.createParallelGroup()
                                .addComponent(pid)
                                .addComponent(startTimeStamp)
                                .addComponent(stopTimeStamp)
                                .addComponent(user)
                                .addComponent(mainClass)
                                .addComponent(javaCommandLine)
                                .addComponent(javaVersion)
                                .addComponent(vmNameAndVersion)
                                .addComponent(vmArguments))
                        .addContainerGap()));

        gl.setVerticalGroup(gl.createSequentialGroup()
                .addContainerGap()
                .addComponent(processSection)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(pidLabel)
                        .addComponent(pid))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(startTimeLabel)
                        .addComponent(startTimeStamp))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(stopTimeLabel)
                        .addComponent(stopTimeStamp))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(userLabel)
                        .addComponent(user))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(javaSection)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(mainClassLabel)
                        .addComponent(mainClass))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(javaCommandLineLabel)
                        .addComponent(javaCommandLine))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(javaVersionLabel)
                        .addComponent(javaVersion))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(vmNameAndVersionLabel)
                        .addComponent(vmNameAndVersion))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(vmArgumentsLabel)
                        .addComponent(vmArguments))
                .addGap(0, 0, Short.MAX_VALUE)
                .addContainerGap());

        container = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        visiblePanel.setContent(container);
    }
}
