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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.swing.experimental.components.ContentPane;
import com.redhat.thermostat.thread.client.swing.experimental.components.DataPane;
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

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final TimelineModel model;
    private final UIDefaults defaults;
    private DataPane controls;

    private JLabel zoomOut;
    private JLabel restoreZoom;
    private JLabel zoomIn;

    public RangeComponentHeader(TimelineModel model, UIDefaults defaults) {
        this.model = model;
        this.defaults = defaults;
    }

    public void initComponents() {
        controls = new DataPane();
        controls.setLayout(new GridLayout(1, 0, 5, 5));

        Icon baseIcon = new FontAwesomeIcon('\uf066', 15, defaults.getIconColor());
        Icon hoverIcon = new FontAwesomeIcon('\uf066', 15,
                                             defaults.getSelectedComponentBGColor());
        zoomOut = new JLabel(baseIcon);
        zoomOut.setToolTipText(t.localize(LocaleResources.ZOOM_OUT).getContents());
        zoomOut.addMouseListener(new Hover(zoomOut, baseIcon, hoverIcon));
        zoomOut.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double ratio = model.getMagnificationRatio();
                model.setMagnificationRatio(ratio/2);
            }
        });
        controls.add(zoomOut);

        baseIcon = new FontAwesomeIcon('\uf03b', 15, defaults.getIconColor());
        hoverIcon = new FontAwesomeIcon('\uf03b', 15,
                                        defaults.getSelectedComponentBGColor());
        restoreZoom = new JLabel(baseIcon);
        restoreZoom.setToolTipText(t.localize(LocaleResources.RESTORE_ZOOM).getContents());
        restoreZoom.addMouseListener(new Hover(restoreZoom, baseIcon, hoverIcon));
        restoreZoom.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                model.setMagnificationRatio(TimelineModel.DEFAULT_RATIO);
            }
        });
        controls.add(restoreZoom);

        baseIcon = new FontAwesomeIcon('\uf065', 15, defaults.getIconColor());
        hoverIcon = new FontAwesomeIcon('\uf065', 15,
                                        defaults.getSelectedComponentBGColor());
        zoomIn = new JLabel(baseIcon);
        zoomIn.setToolTipText(t.localize(LocaleResources.ZOOM_IN).getContents());
        zoomIn.addMouseListener(new Hover(zoomIn, baseIcon, hoverIcon));
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

    public void setControlsEnabled(boolean b) {
        zoomIn.setEnabled(b);
        zoomOut.setEnabled(b);
        restoreZoom.setEnabled(b);
    }

    private class Hover extends MouseAdapter {

        private final JLabel label;
        private final Icon baseIcon;
        private final Icon hoverIcon;

        public Hover(JLabel label, Icon baseIcon, Icon hoverIcon) {

            this.label = label;
            this.baseIcon = baseIcon;
            this.hoverIcon = hoverIcon;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            onMouseHover(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            onMouseHover(false);
        }

        public void onMouseHover(boolean hover) {
            label.setIcon(hover ? hoverIcon : baseIcon);
            repaint();
        }
    }
}
