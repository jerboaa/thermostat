/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.application.gui.internal;

import com.redhat.thermostat.client.swing.MenuHelper;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.client.ui.MenuRegistry;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import javax.swing.JMenuBar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class MenuHandler {

    private static final Logger logger = LoggingUtils.getLogger(MenuHandler.class);

    private MenuHelper menuHelper;

    private MenuRegistry registry;
    private JMenuBar menuBar;

    private MenuListener listener;

    public void startService() {
        BundleContext context =
                FrameworkUtil.getBundle(MenuHandler.class).getBundleContext();
        try {
            registry = new MenuRegistry(context);
            listener = new MenuListener();

            registry.addActionListener(listener);
            registry.start();

        } catch (InvalidSyntaxException itJustWontHappen) {
            throw new RuntimeException(itJustWontHappen);
        }
    }

    public void initComponents(CommonPaths commonPaths) {
        menuBar = new JMenuBar();
        menuHelper = new MenuHelper(commonPaths, menuBar);
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    private void removeMenu(MenuAction action) {
        menuHelper.removeMenuAction(action);
    }

    private void addMenu(MenuAction action) {
        menuHelper.addMenuAction(action);
    }

    public void saveState() {
        menuHelper.saveMenuStates();
    }

    private class MenuListener implements ActionListener<ThermostatExtensionRegistry.Action>
    {
        @Override
        public void actionPerformed(
                ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
            MenuAction action = (MenuAction) actionEvent.getPayload();

            switch (actionEvent.getActionId()) {
                case SERVICE_ADDED:
                    addMenu(action);
                    break;

                case SERVICE_REMOVED:
                    removeMenu(action);
                    break;

                default:
                    logger.log(Level.WARNING, "received unknown event from MenuRegistry: " +
                            actionEvent.getActionId());
                    break;
            }
        }
    }
}
