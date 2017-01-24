/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal;

import com.redhat.thermostat.client.swing.components.AbstractLayout;
import com.redhat.thermostat.client.swing.internal.TabbedPane.TabController;

import javax.swing.JComponent;
import java.awt.Container;
import java.awt.Dimension;
import java.util.List;

/**
 */
class TabbedPaneControlPanelLayout extends AbstractLayout {

    private int hgap;
    private int vgap;

    public TabbedPaneControlPanelLayout() {
        hgap = 5;
        vgap = 5;
    }

    @Override
    public Dimension preferredLayoutSize(Container tabControls) {
        TabbedPane tabbedPane = (TabbedPane) tabControls.getParent();
        int totalWidth = tabbedPane.getWidth();

        int height = 0;
        for (Tab tab : tabbedPane.getTabs()) {
            Dimension tabSize = tab.getPreferredSize();
            height = Math.max(height, tabSize.height);
        }

        return new Dimension(totalWidth, height + vgap + vgap);
    }

    @Override
    protected void doLayout(Container tabControls) {
        TabbedPane tabbedPane = (TabbedPane) tabControls.getParent();

        TabController controller = tabbedPane.getController();
        List<Tab> hiddenTabsList = controller.getHiddenTabs();
        hiddenTabsList.clear();

        JComponent hiddenTabs = tabbedPane.getHiddenTabsControl();
        hiddenTabs.setBounds(-5, 0, 0, 0);

        int totalWidth = tabbedPane.getWidth();

        int height = 0;
        int tabsSpan = 0;
        for (Tab tab : tabbedPane.getTabs()) {
            Dimension tabSize = tab.getPreferredSize();
            height = Math.max(height, tabSize.height);

            tabsSpan += tabSize.width + hgap;
        }

        tabControls.setBounds(0, 0, totalWidth, height + vgap + vgap);

        if (tabsSpan <= totalWidth) {
            int x = totalWidth / 2 - tabsSpan / 2;
            for (Tab tab : tabbedPane.getTabs()) {
                Dimension tabSize = tab.getPreferredSize();
                tab.setBounds(x, vgap, tabSize.width, tabSize.height);
                x += tabSize.width + hgap;
            }

        } else {

            int farSide = height + vgap + vgap;
            hiddenTabs.setBounds(totalWidth - height, 0, height, farSide);

            boolean hideRest = false;
            int x = hgap;
            for (Tab tab : tabbedPane.getTabs()) {

                if (hideRest) {
                    tab.setBounds(-5, 0, 0, 0);
                    hiddenTabsList.add(tab);
                    continue;
                }

                Dimension tabSize = tab.getPreferredSize();
                int nextX = x + tabSize.width + hgap;
                if (nextX > (totalWidth - farSide - hgap)) {
                    tab.setBounds(-5, 0, 0, 0);
                    hiddenTabsList.add(tab);

                    hideRest = true;
                    continue;
                }

                tab.setBounds(x, vgap, tabSize.width, tabSize.height);
                x = nextX;
            }
        }
    }
}
