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

package com.redhat.thermostat.client.swing.views;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.LayoutStyle.ComponentPlacement;

import com.redhat.thermostat.client.swing.IconResource;


public class MemorySpacePanel extends JPanel {

    private final JProgressBar percentagePanel;
    private final JLabel additionalDetailsIcon;
    private final JLabel lblUsed;
    private final JLabel lblAvailable;

    public MemorySpacePanel(String regionName) {
        JLabel lblRegionName = new JLabel(regionName);

        percentagePanel = new JProgressBar(0, 100);

        additionalDetailsIcon = new JLabel(IconResource.ARROW_RIGHT.getIcon());

        lblUsed = new JLabel("${USED}");
        lblAvailable = new JLabel("${AVAILABLE}");

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblRegionName)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(percentagePanel, GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addComponent(lblUsed)
                                    .addPreferredGap(ComponentPlacement.RELATED, 441, Short.MAX_VALUE)
                                    .addComponent(lblAvailable)))))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(additionalDetailsIcon)
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblRegionName)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(additionalDetailsIcon, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(percentagePanel, GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblUsed)
                        .addComponent(lblAvailable))
                    .addGap(5))
        );
        setLayout(groupLayout);
    }


    public void updateRegionData(int percentageUsed, String currentlyUsed, String currentlyAvailable, String allocatable) {
        percentagePanel.setValue(percentageUsed);

        lblUsed.setText(currentlyUsed);
        lblAvailable.setText(currentlyAvailable);
        additionalDetailsIcon.setToolTipText(allocatable);
    }

}
