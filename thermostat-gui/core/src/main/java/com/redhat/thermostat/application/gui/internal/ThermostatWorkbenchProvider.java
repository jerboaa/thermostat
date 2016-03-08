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

import com.redhat.thermostat.application.gui.ThermostatGUI;
import com.redhat.thermostat.platform.mvc.Controller;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.mvc.Model;
import com.redhat.thermostat.platform.mvc.View;
import com.redhat.thermostat.platform.swing.SwingWorkbench;
import com.redhat.thermostat.platform.swing.WorkbenchView;
import com.redhat.thermostat.shared.config.CommonPaths;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 */
@Component
@Service({ ThermostatWorkbenchProvider.class, MVCProvider.class })
public class ThermostatWorkbenchProvider implements MVCProvider {

    @Reference(bind = "bindCommonPaths", unbind = "unbindCommonPaths")
    private CommonPaths paths;

    @Reference(bind = "bindWorkbench", unbind = "unbindWorkbench")
    private SwingWorkbench workbench;

    @Reference(bind = "bindThermostatGUI", unbind = "unbindThermostatGUI")
    private ThermostatGUI thermostatGUI;

    protected void bindCommonPaths(CommonPaths paths) {
        this.paths = paths;
    }

    protected void unbindCommonPaths(CommonPaths paths) {
        this.paths = null;
    }

    protected void bindWorkbench(SwingWorkbench workbench) {
        this.workbench = workbench;
    }

    protected void unbindWorkbench(SwingWorkbench workbench) {
        this.workbench = null;
    }

    protected void bindThermostatGUI(ThermostatGUI thermostatGUI) {
        this.thermostatGUI = thermostatGUI;
    }

    protected void unbindThermostatGUI(ThermostatGUI thermostatGUI) {
        this.thermostatGUI = null;
    }

    private Model model;
    private ThermostatWorkbenchView view;
    private ThermostatWorkbenchController controller;

    @Override
    public Controller getController() {
        return controller;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Activate
    public void activate() {
        model = new Model();

        controller = new ThermostatWorkbenchController();
        controller.setCommonPaths(paths);

        view = new ThermostatWorkbenchView((WorkbenchView) workbench.getView());
    }
}
