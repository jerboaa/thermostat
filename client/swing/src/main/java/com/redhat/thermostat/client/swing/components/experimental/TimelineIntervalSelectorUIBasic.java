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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.redhat.thermostat.client.swing.components.experimental.TimelineIntervalMouseHandler.TimeIntervalSelectorTarget;
import com.redhat.thermostat.client.swing.components.experimental.TimelineIntervalSelectorModel.ChangeListener;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.model.Range;

public class TimelineIntervalSelectorUIBasic extends TimelineIntervalSelectorUI implements TimeIntervalSelectorTarget {

    private static final int SIDE_PADDING = 10;

    // the extra gap below the timeline header itself
    private static final int GAP_BELOW = 15; /* pixels */

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
    private PropertyChangeListener enabledListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("enabled")) {
                boolean enabled = (Boolean) evt.getNewValue();
                timeline.setEnabled(enabled);
                if (enabled) {
                    addUserInputListeners();
                } else {
                    removeUserInputListeners();
                }
            }
        }
    };

    public TimelineIntervalSelectorUIBasic() {
        customPaintingPanel.setLayout(new BorderLayout());
        customPaintingPanel.setBorder(new EmptyBorder(0, SIDE_PADDING, 0, SIDE_PADDING));
    }

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

        component.setSelectionLinePaint(Color.BLACK);
    }

    protected void installComponents() {
        topGlue = Box.createVerticalGlue();
        bottomGlue = Box.createVerticalGlue();

        customPaintingPanel.add(timeline, BorderLayout.CENTER);

        component.add(topGlue);
        component.add(customPaintingPanel);
        component.add(bottomGlue);
    }

    protected void installListeners() {
        component.getModel().addChangeListener(timelineSelectionPainter);
        component.getModel().addChangeListener(timelineRangeUpdater);

        component.addPropertyChangeListener(enabledListener);

        addUserInputListeners();
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
        customPaintingPanel.remove(timeline);

        component.remove(bottomGlue);
        component.remove(customPaintingPanel);
        component.remove(topGlue);
    }

    protected void uninstallListeners() {
        removeUserInputListeners();

        component.getModel().removeChangeListener(timelineRangeUpdater);
        component.getModel().removeChangeListener(timelineSelectionPainter);
    }

    protected void uninstallDefaults() {
        component.setSelectionLinePaint(null);

        component.setLayout(null);
    }

    private void removeUserInputListeners() {
        component.removeMouseWheelListener(mouseListener);
        component.removeMouseMotionListener(mouseListener);
        component.removeMouseListener(mouseListener);
    }

    private void addUserInputListeners() {
        component.addMouseListener(mouseListener);
        component.addMouseMotionListener(mouseListener);
        component.addMouseWheelListener(mouseListener);
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
        int width = timeline.getWidth();
        return (int) (1.0 * (domainValue - domainMin) / (domainMax - domainMin) * (width - 1));
    }

    private long xToDomain(int x) {
        long domainMin = component.getModel().getTotalMinimum();
        long domainMax = component.getModel().getTotalMaximum();
        int width = timeline.getWidth();
        return (long) ((1.0 * x / (width - 1) * (domainMax - domainMin)) + domainMin);
    }

    private class CustomPaintPanel extends JPanel {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, timeline.getPreferredSize().height + GAP_BELOW);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            Graphics2D g2 = (Graphics2D) g.create();

            int left = domainToX(component.getModel().getSelectedMinimum()) + SIDE_PADDING ;
            int right = domainToX(component.getModel().getSelectedMaximum()) + SIDE_PADDING;
            int height = getHeight();
            int width = getWidth() - 1;

            boolean enabled = component.isEnabled();
            if (enabled) {
                g2.setPaint(component.getSelectionLinePaint());
            } else {
                g2.setPaint(Color.LIGHT_GRAY);
            }

            int pinchHeight = getHeight() - (GAP_BELOW / 2);

            g2.drawLine(0, height, 0, pinchHeight);
            g2.drawLine(0, pinchHeight, left, pinchHeight);
            g2.drawLine(left, pinchHeight, left, 0);
            paintHandle(g2, left, pinchHeight/2);

            g2.drawLine(width, height, width, pinchHeight);
            g2.drawLine(width, pinchHeight, right, pinchHeight);
            g2.drawLine(right, pinchHeight, right, 0);
            paintHandle(g2, right, pinchHeight/2);

            g2.dispose();
        }

        private void paintHandle(Graphics2D g, int x, int y) {
            g = (Graphics2D) g.create();
            g.translate(x, y);

            g.setColor(Palette.LIGHT_GRAY.getColor());
            g.fill(new RoundRectangle2D.Float(-2, -10, 4, 20, 2, 2));

            if (component.isEnabled()) {
                g.setPaint(component.getSelectionLinePaint());
            } else {
                g.setPaint(Color.LIGHT_GRAY);
            }

            g.draw(new RoundRectangle2D.Float(-2, -10, 4, 20, 2, 2));

            g.dispose();
        }
    }
}
