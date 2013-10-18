/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Thermostat; see the file COPYING. If not see <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work based on this
 * code. Thus, the terms and conditions of the GNU General Public License cover
 * the whole combination.
 *
 * As a special exception, the copyright holders of this code give you
 * permission to link this code with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this code. If you modify this
 * code, you may extend this exception to your version of the library, but you
 * are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package com.redhat.thermostat.client.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.redhat.thermostat.client.swing.components.TimelineIntervalMouseHandler.TimeIntervalSelectorTarget;
import com.redhat.thermostat.client.swing.components.TimelineIntervalSelectorModel.ChangeListener;
import com.redhat.thermostat.client.swing.components.timeline.Timeline;
import com.redhat.thermostat.common.model.Range;

public class TimelineIntervalSelectorUIBasic extends TimelineIntervalSelectorUI implements TimeIntervalSelectorTarget {

    private static final int PREF_HEIGHT = 20; /* pixels */

    private TimelineIntervalSelector component;

    private TimelineIntervalMouseHandler mouseListener = new TimelineIntervalMouseHandler(this);
    private Timeline timeline = new Timeline(new Range<Long>(0l, 100l));
    private JPanel customPaintingPanel = new CustomPaintPanel();

    private Component topGlue;
    private Component bottomGlue;

    private ChangeListener timelineSelectionPainter = new ChangeListener() {
        @Override
        public void changed() {
            component.repaint();
        }
    };
    private ChangeListener timelineRangeUpdater = new ChangeListener() {
        @Override
        public void changed() {
            TimelineIntervalSelectorModel model = component.getModel();
            timeline.setRange(new Range<>(model.getTotalMinimum(), model.getTotalMaximum()));
        }
    };

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        component = (TimelineIntervalSelector) c;

        installDefaults();
        installComponents();
        installListeners();

    }

    protected void installDefaults() {
        component.setLayout(new BoxLayout(component, BoxLayout.PAGE_AXIS));
        component.setBorder(new EmptyBorder(5,5,5,5));
    }

    protected void installComponents() {
        topGlue = Box.createVerticalGlue();
        bottomGlue = Box.createVerticalGlue();

        component.add(topGlue);
        component.add(timeline);
        component.add(customPaintingPanel);
        component.add(bottomGlue);
    }

    protected void installListeners() {
        component.getModel().addChangeListener(timelineSelectionPainter);
        component.getModel().addChangeListener(timelineRangeUpdater);

        component.addMouseListener(mouseListener);
        component.addMouseMotionListener(mouseListener);
        component.addMouseWheelListener(mouseListener);
    }

    @Override
    public void uninstallUI(JComponent c) {
        uninstallListeners();
        uninstallComponents();
        uninstallDefaults();

        component = null;

        super.uninstallUI(c);
    }

    protected void uninstallComponents() {
        component.remove(bottomGlue);
        component.remove(customPaintingPanel);
        component.remove(timeline);
        component.remove(topGlue);
    }

    protected void uninstallListeners() {
        component.removeMouseWheelListener(mouseListener);
        component.removeMouseMotionListener(mouseListener);
        component.removeMouseListener(mouseListener);

        component.getModel().removeChangeListener(timelineRangeUpdater);
        component.getModel().removeChangeListener(timelineSelectionPainter);
    }

    protected void uninstallDefaults() {
        component.setLayout(null);
    }

    @Override
    public int getLeftSelectionPosition() {
        return domainToX(component.getModel().getSelectedMinimum());
    }

    @Override
    public int getRightSelectionPosition() {
        return domainToX(component.getModel().getSelectedMaximum());
    }

    @Override
    public int getSelectionMargin() {
        return 20;
    }

    @Override
    public void setCursor(Cursor cursor) {
        component.setCursor(cursor);
    }

    @Override
    public void updateSelectionPosition(int left, int right) {
        long min = xToDomain(left);
        long max = xToDomain(right);
        component.getModel().setSelectedMaximum(max);
        component.getModel().setSelectedMinimum(min);

        component.repaint();
    }

    private int domainToX(long domainValue) {
        long domainMin = component.getModel().getTotalMinimum();
        long domainMax = component.getModel().getTotalMaximum();
        int width = component.getWidth();
        return (int) (1.0 * (domainValue - domainMin) / (domainMax - domainMin) * width);
    }

    private long xToDomain(int x) {
        long domainMin = component.getModel().getTotalMinimum();
        long domainMax = component.getModel().getTotalMaximum();
        int width = component.getWidth();
        return (long) ((1.0 * x / (width) * (domainMax - domainMin)) + domainMin);
    }

    private class CustomPaintPanel extends JPanel {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().height, PREF_HEIGHT);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            Graphics2D g2 = (Graphics2D) g.create();

            int startX = domainToX(component.getModel().getSelectedMinimum());
            int endX = domainToX(component.getModel().getSelectedMaximum());

            g2.fillRect(startX, 0, (endX - startX), getHeight());

            g2.dispose();
        }
    }
}
