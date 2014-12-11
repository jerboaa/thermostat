/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.thread.client.swing.experimental.components.ContentPane;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;

/**
 *
 */
public class RangeComponentHeader extends ContentPane {

    private final TimelineModel model;
    private final UIDefaults defaults;
    private ContentPane controls;

    public RangeComponentHeader(TimelineModel model, UIDefaults defaults) {
        this.model = model;
        this.defaults = defaults;
    }

    public void initComponents() {
        controls = new ContentPane();
        controls.setLayout(new GridLayout(1, 0, 5, 5));

        JLabel zoomOut = new JLabel(new FontAwesomeIcon('\uf066', 15,
                                                        defaults.getIconColor()));
        zoomOut.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double ratio = model.getMagnificationRatio();
                model.setMagnificationRatio(ratio/2);
            }
        });
        controls.add(zoomOut);

        JLabel restoreZoom = new JLabel(new FontAwesomeIcon('\uf03b', 15,
                                                        defaults.getIconColor()));
        restoreZoom.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                model.setMagnificationRatio(TimelineModel.DEFAULT_RATIO);
            }
        });
        controls.add(restoreZoom);

        JLabel zoomIn = new JLabel(new FontAwesomeIcon('\uf065', 15,
                                                       defaults.getIconColor()));
        zoomIn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double ratio = model.getMagnificationRatio();
                model.setMagnificationRatio(ratio*2);
            }
        });
        controls.add(zoomIn);

        add(controls, BorderLayout.EAST);
    }
}
