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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.Component;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.client.ui.SwingComponent;
import com.redhat.thermostat.thread.client.common.VMThreadCapabilitiesView;
import com.redhat.thermostat.thread.collector.VMThreadCapabilities;

public class SwingVMThreadCapabilitiesView extends VMThreadCapabilitiesView implements SwingComponent {

    private VMCapsSummaryPanel panel;
    
    public SwingVMThreadCapabilitiesView() {
        panel = new VMCapsSummaryPanel();
        panel.addHierarchyListener(new ComponentVisibleListener() {
            
            @Override
            public void componentShown(Component component) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        SwingVMThreadCapabilitiesView.this.notify(Action.VISIBLE);
                        return null;
                    }
                };
                worker.execute();
            }
            
            @Override
            public void componentHidden(Component component) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        SwingVMThreadCapabilitiesView.this.notify(Action.HIDDEN);
                        return null;
                    }
                };
                worker.execute();
            }
        });
    }
    
    @Override
    public void setVMThreadCapabilities(final VMThreadCapabilities caps) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                panel.addInfo(caps.getSupportedFeaturesList());
            }
        });
    }
    
    @Override
    public Component getUiComponent() {
        return panel;
    }
}
