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

package com.redhat.thermostat.client.swing.components;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.redhat.thermostat.client.swing.components.experimental.RecentTimeControlPanel;
import org.jfree.chart.ChartPanel;

import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.Duration;

public class RecentTimeSeriesChartPanel extends JPanel {

    private static final long serialVersionUID = -1733906800911900456L;
    private static final int MINIMUM_DRAW_SIZE = 100;

    private RecentTimeControlPanel recentTimeControlPanel;
    private JTextComponent label;

    public RecentTimeSeriesChartPanel(final RecentTimeSeriesChartController controller) {

        this.setLayout(new BorderLayout());

        final ChartPanel cp = controller.getChartPanel();

        cp.setDisplayToolTips(false);
        cp.setDoubleBuffered(true);
        cp.setMouseZoomable(false);
        cp.setPopupMenu(null);

        /*
         * By default, ChartPanel scales itself instead of redrawing things when
         * it's resized. To have it resize automatically, we need to set minimum
         * and maximum sizes. Lets constrain the minimum, but not the maximum
         * size.
         */
        cp.setMinimumDrawHeight(MINIMUM_DRAW_SIZE);
        cp.setMaximumDrawHeight(Integer.MAX_VALUE);
        cp.setMinimumDrawWidth(MINIMUM_DRAW_SIZE);
        cp.setMaximumDrawWidth(Integer.MAX_VALUE);
        Duration duration = new Duration(controller.getTimeValue(), controller.getTimeUnit());
        recentTimeControlPanel = new RecentTimeControlPanel(duration);
        recentTimeControlPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                Duration d = (Duration) evt.getNewValue();
                controller.setTime(d.getValue(), d.getUnit());
            }
        });
        add(recentTimeControlPanel, BorderLayout.SOUTH);

        add(cp, BorderLayout.CENTER);
    }

    public void setDataInformationLabel(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label == null) {
                    label = new ValueField(text);
                    recentTimeControlPanel.addTextComponent(label);
                }

                label.setName("crossHair");
                label.setText(text);
            }
        });
    }

}

