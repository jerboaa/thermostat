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
import java.util.HashMap;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.locale.LocaleResources;

public class VmMemoryPanel extends JPanel implements VmMemoryView {

    private static final long serialVersionUID = -2882890932814218436L;

    private final Map<String, MemorySpacePanel> regions = new HashMap<>();

    private final JPanel currentRegionSizePanel;

    public VmMemoryPanel() {
        JLabel lblMem = new JLabel(localize(LocaleResources.VM_MEMORY_SPACE_TITLE));

        currentRegionSizePanel = new JPanel();

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(currentRegionSizePanel, GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
                            .addContainerGap())
                        .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                            .addComponent(lblMem)
                            .addGap(491))))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblMem)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(currentRegionSizePanel, GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                    .addContainerGap())
        );
        currentRegionSizePanel.setLayout(new BoxLayout(currentRegionSizePanel, BoxLayout.PAGE_AXIS));
        setLayout(groupLayout);

    }

    @Override
    public void addRegion(final String humanReadableName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MemorySpacePanel regionInfo = new MemorySpacePanel(humanReadableName);
                regions.put(humanReadableName, regionInfo);
                currentRegionSizePanel.add(regionInfo);
                currentRegionSizePanel.revalidate();
            }
        });
    }


    @Override
    public void removeAllRegions() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                regions.clear();
                currentRegionSizePanel.removeAll();
                currentRegionSizePanel.revalidate();
            }
        });
    }

    @Override
    public void updateRegionSize(final String name, final int percentageUsed, final String currentlyUsed, final String currentlyAvailable, final String allocatable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                regions.get(name).updateRegionData(percentageUsed, currentlyUsed, currentlyAvailable, allocatable);
            }
        });

    }

    @Override
    public Component getUiComponent() {
        return this;
    }

}
