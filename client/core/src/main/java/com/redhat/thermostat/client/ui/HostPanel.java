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

import com.redhat.thermostat.client.internal.HostPanelFacade;
import com.redhat.thermostat.client.locale.LocaleResources;

public class HostPanel extends JPanel {

    /*
     * This entire class needs to be more dynamic. We should try to avoid
     * creating objects and should just update them when necessary
     */

    private static final long serialVersionUID = 4835316442841009133L;

    private final HostPanelFacade facade;

    public HostPanel(final HostPanelFacade facade) {
        this.facade = facade;

        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();

        // FIXME: Fix how we get old of the view impl specific UI component.
        tabPane.insertTab(localize(LocaleResources.HOST_INFO_TAB_OVERVIEW), null, ((HostOverviewPanel)facade.getOverviewController().getView()).getUiComponent(), null, 0);
        tabPane.insertTab(localize(LocaleResources.HOST_INFO_TAB_CPU), null, ((HostCpuPanel)facade.getCpuController().getView()).getUiComponent(), null, 1);
        tabPane.insertTab(localize(LocaleResources.HOST_INFO_TAB_MEMORY), null, ((HostMemoryPanel)facade.getMemoryController().getView()).getUiComponent(), null, 2);

        // TODO additional tabs provided by plugins
        // tabPane.insertTab(title, icon, component, tip, 3)

        this.add(tabPane);

    }

}
