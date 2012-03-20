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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.redhat.thermostat.client.VmPanelFacade;
import com.redhat.thermostat.client.locale.LocaleResources;


public class VmPanel extends JPanel {

    private static final long serialVersionUID = 2816226547554943368L;

    private final VmPanelFacade facade;

    public VmPanel(final VmPanelFacade facade) {
        this.facade = facade;
        createUI();

        addHierarchyListener(new AsyncFacadeManager(facade));
    }

    public void createUI() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();

        tabPane.insertTab(localize(LocaleResources.VM_INFO_TAB_OVERVIEW), null, facade.getOverviewController().getComponent(), null, 0);
        tabPane.insertTab(localize(LocaleResources.VM_INFO_TAB_MEMORY), null, facade.getMemoryController().getComponent(), null, 1);
        tabPane.insertTab(localize(LocaleResources.VM_INFO_TAB_GC), null, facade.getGcController().getComponent(),
                          localize(LocaleResources.GARBAGE_COLLECTION), 2);
        tabPane.insertTab(localize(LocaleResources.VM_INFO_TAB_CLASSES), null, facade.getClassesController().getComponent(), null, 3);

        // TODO additional tabs provided by plugins
        // tabPane.insertTab(title, icon, component, tip, 3)

        this.add(tabPane);
    }


}
