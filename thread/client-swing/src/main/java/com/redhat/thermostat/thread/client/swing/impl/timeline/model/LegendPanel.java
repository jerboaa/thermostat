/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline.model;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.thread.client.common.chart.ChartColors;
import com.redhat.thermostat.thread.client.swing.experimental.components.DataPane;
import com.redhat.thermostat.thread.client.swing.experimental.components.Separator;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 *
 */
public class LegendPanel extends DataPane {

    public LegendPanel(UIDefaults defaults) {
        setBorder(new Separator(defaults, Separator.Side.TOP.TOP, Separator.Type.SOLID));
        setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        setPreferredSize(new Dimension(getWidth(), 30));

        for (Thread.State state : Thread.State.values()) {

            Color color = ChartColors.getColor(state);
            // no chart is black, it's just the default colour
            if (!color.equals(Color.BLACK)) {
                JLabel label =  new JLabel(new ColorIcon(color), SwingConstants.LEFT);
                label.setText(state.toString());
                add(label);
            }
        }
    }

    private class ColorIcon implements Icon {

        private Color color;
        private ColorIcon(Color color) {
            this.color = color;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
            graphics.setColor(color);
            graphics.fillRect(x, y, getIconWidth(), getIconHeight());
            graphics.dispose();
        }
    }
}
