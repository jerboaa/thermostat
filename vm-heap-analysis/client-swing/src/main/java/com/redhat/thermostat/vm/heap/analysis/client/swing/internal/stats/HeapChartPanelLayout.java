/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;

import com.redhat.thermostat.client.swing.components.AbstractLayout;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.model.LongRangeNormalizer;

public class HeapChartPanelLayout extends AbstractLayout {

    @Override
    protected void doLayout(Container parent) {
        
        HeapChartPanel chartPanel = (HeapChartPanel) parent;
        Rectangle2D area = chartPanel.getScreenDataArea();
        
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();

        // need first and last value
        
        long max = domainAxis.getMaximumDate().getTime();
        long min = domainAxis.getMinimumDate().getTime();

        Range<Long> offset = new Range<Long>(min, max);
        
        LongRangeNormalizer normaliser = new LongRangeNormalizer(offset);
        chartPanel.getScreenDataArea();
        
        normaliser.setMaxNormalized((int) (area.getX() + area.getWidth()));
        normaliser.setMinNormalized((int) area.getX());
        
        int y = (int) (area.getHeight()/2);
        int bound = y;
        
        boolean moveUp = false;
        int delta = 0;
        int x = 0;
        
        Component[] children = chartPanel.getComponents();
        for (Component _child : children) {
            
            if (!(_child instanceof OverlayComponent)) {
                continue;
            }
            
            OverlayComponent child = (OverlayComponent) _child;
            
            if (!child.isVisible()) {
                continue;
            }
            
            Dimension preferredSize = child.getIconCenter();

            normaliser.setValue(child.getTimestamp());
            x = (int) normaliser.getValueNormalized() - preferredSize.width;

            preferredSize = child.getPreferredSize();
            Rectangle bounds = new Rectangle(x, y, preferredSize.width, preferredSize.height);

            if (delta > bound) {
                delta = 0;
            }

            if (moveUp) {
                bounds.y = bounds.y - delta;
            } else {
                bounds.y = bounds.y + delta;
                delta += bounds.height;
            }
            moveUp = !moveUp;

            child.setSize(preferredSize);
            child.setBounds(bounds);
        }
    }

}
