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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.redhat.thermostat.client.swing.components.experimental.EventTimelineModel.Event;
import com.redhat.thermostat.client.swing.components.experimental.TimelineIntervalMouseHandler.TimeIntervalSelectorTarget;
import com.redhat.thermostat.client.swing.internal.LocaleResources;
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.Translate;

public class BasicEventTimelineUI extends EventTimelineUI {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private static final Color DEFAULT_FILL_COLOR = new Color(0,0,0,255);
    private static final Color DEFAULT_EDGE_COLOR = Color.BLACK;
    private static final Color DEFAULT_MARKER_COLOR = Color.BLACK;

    private EventTimeline eventTimeline;

    private OverviewPanel overviewPanel = new OverviewPanel();
    private Timeline overviewRuler;

    private Refresher refresher = new Refresher(overviewPanel);
    private JButton moveLeftButton;
    private JButton moveRightButton;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton resetZoomButton;

    @Override
    protected void installComponents(EventTimeline component) {
        eventTimeline = component;

        overviewRuler = new Timeline(new Range<>(1L, 2L));

        moveLeftButton = new JButton("<");
        moveLeftButton.setName("moveLeftButton");
        moveLeftButton.setMargin(new Insets(0, 0, 0, 0));
        moveRightButton = new JButton(">");
        moveRightButton.setName("moveRightButton");
        moveRightButton.setMargin(new Insets(0, 0, 0, 0));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout());

        zoomInButton = new JButton("+");
        zoomInButton.setName("zoomInButton");
        zoomInButton.setToolTipText(translate.localize(LocaleResources.ZOOM_IN).getContents());
        zoomInButton.setMargin(new Insets(2, 2, 2, 2));

        buttonPanel.add(zoomInButton);

        zoomOutButton = new JButton("-");
        zoomOutButton.setName("zoomOutButton");
        zoomOutButton.setToolTipText(translate.localize(LocaleResources.ZOOM_OUT).getContents());
        zoomOutButton.setMargin(new Insets(2, 2, 2, 2));
        buttonPanel.add(zoomOutButton);

        resetZoomButton = new JButton("R");
        resetZoomButton.setName("zoomResetButton");
        resetZoomButton.setToolTipText(translate.localize(LocaleResources.RESET_ZOOM).getContents());
        resetZoomButton.setMargin(new Insets(2, 2, 2, 2));
        buttonPanel.add(resetZoomButton);

        GridBagLayout layout = new GridBagLayout();
        component.setLayout(layout);
        overviewPanel.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        c.weighty = 1;

        component.add(moveLeftButton, c);

        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 1.0;

        component.add(overviewPanel, c);

        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        component.add(overviewRuler, c);

        c.gridx = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        c.weightx = 0;
        component.add(moveRightButton, c);

        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 4;
        component.add(buttonPanel, c);
    }

    @Override
    protected void installDefaults(EventTimeline c) {
        c.setSelectionEdgePaint(DEFAULT_EDGE_COLOR);
        c.setSelectionFillPaint(DEFAULT_FILL_COLOR);
        c.setEventPaint(DEFAULT_MARKER_COLOR);
    }

    @Override
    protected void installListeners(EventTimeline c) {
        c.addHierarchyBoundsListener(refresher);
        c.addHierarchyListener(refresher);
        c.getModel().addDataChangeListener(refresher);
        c.getModel().addRangeChangeListener(new EventTimelineRangeChangeListener() {
            @Override
            public void rangeChanged(Range<Long> overview, Range<Long> detail) {
                overviewRuler.setRange(overview);
                overviewRuler.repaint();

            }
        });
        moveRightButton.addActionListener(new DetailChangeListener() {
            @Override
            protected Range<Long> computeNewDetailRange(long min, long max) {
                long diff = (long) ((max - min) * 0.1);
                return new Range<>(min + diff, max + diff);
            }
        });
        moveLeftButton.addActionListener(new DetailChangeListener() {
            protected Range<Long> computeNewDetailRange(long min, long max) {
                long diff = (long) ((max - min) * 0.1);
                return new Range<>(min - diff, max - diff);
            }
        });
        zoomOutButton.addActionListener(new DetailChangeListener() {
           protected Range<Long> computeNewDetailRange(long min, long max) {
               long diff = max - min;
               return new Range<>(min - diff / 2, max + diff / 2);
           }
        });

        zoomInButton.addActionListener(new DetailChangeListener() {
            protected Range<Long> computeNewDetailRange(long min, long max) {
                long diff = max - min;
                return new Range<>(min + diff / 4, max - diff / 4);
            }
        });

        resetZoomButton.addActionListener(new DetailChangeListener() {
            @Override
            protected Range<Long> computeNewDetailRange(long min, long max) {
                long timeDelta = max - min;

                long tenMinutesInMillis = TimeUnit.MINUTES.toMillis(10);

                if (timeDelta <= tenMinutesInMillis) {
                    return new Range<>(min, max);
                } else {
                    return new Range<>(max - tenMinutesInMillis, max);
                }
            }
        });
    }

    private abstract class DetailChangeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            EventTimelineModel model = eventTimeline.getModel();

            if (model.getEvents().isEmpty()) {
                return;
            }

            Range<Long> range = model.getDetailRange();

            long min = range.getMin();
            long max = range.getMax();

            model.setDetailRange(computeNewDetailRange(min, max));

            overviewPanel.refresh();
        }

        protected abstract Range<Long> computeNewDetailRange(long min, long max);
    }

    protected void uninstallListeners(EventTimeline c) {
        c.removeHierarchyBoundsListener(refresher);
        c.removeHierarchyListener(refresher);
    }

    @Override
    protected void uninstallComponents(EventTimeline c) {
        c.remove(overviewPanel);

        eventTimeline = null;
    }

    private long positionToTimeStamp(int position) {
        Range<Long> range = eventTimeline.getModel().getTotalRange();
        LongRangeNormalizer normalizer = new LongRangeNormalizer(new Range<>(0L, (long)overviewPanel.getWidth()), range);
        return normalizer.getValueNormalized(position);
    }

    private int timeStampToPosition(long timeStamp) {
        Range<Long> range = eventTimeline.getModel().getTotalRange();
        LongRangeNormalizer normalizer = new LongRangeNormalizer(range, new Range<>(0L, (long)overviewPanel.getWidth()));
        return (int) normalizer.getValueNormalized(timeStamp);
    }

    private static class Refresher implements HierarchyBoundsListener, HierarchyListener, AdjustmentListener, EventTimelineDataChangeListener {

        private OverviewPanel toRefresh;

        public Refresher(OverviewPanel toRefresh) {
            this.toRefresh = toRefresh;
        }

        @Override
        public void dataChanged() {
            refresh();
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            refresh();
        }

        @Override
        public void ancestorMoved(HierarchyEvent e) {
            refresh();
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            refresh();
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            refresh();
        }

        private void refresh() {
            toRefresh.refresh();
        }
    }

    private class OverviewPanel extends JPanel implements TimeIntervalSelectorTarget {

        private int MOUSE_MARGIN = 10;

        private int left;
        private int right;

        public OverviewPanel() {
            TimelineIntervalMouseHandler chartMotionListener = new TimelineIntervalMouseHandler(this);
            addMouseMotionListener(chartMotionListener);
            addMouseListener(chartMotionListener);
        }

        public void refresh() {
            recomputeBars();
            repaint();
        }

        private void recomputeBars() {
            Range<Long> range = eventTimeline.getModel().getDetailRange();
            if (range != null) {
                left = timeStampToPosition(range.getMin());
                right = timeStampToPosition(range.getMax());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = (right - left);
            g.clearRect(0, 0, getWidth(), getHeight());

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            EventTimelineModel model = eventTimeline.getModel();

            int i = 0;
            for (Event event : model.getEvents()) {
                paintEvent(g2, event, i);
                i++;
            }

            Paint fillColor = eventTimeline.getSelectionFillPaint();
            g2.setPaint(fillColor);
            g2.fillRect(left, 1, width, getHeight() - 2);

            g2.setStroke(new BasicStroke(2));
            Paint edgeColor = eventTimeline.getSelectionEdgePaint();
            g2.setPaint(edgeColor);
            g2.drawRect(left, 1, width, getHeight() - 2);

            g2.dispose();
        }

        private void paintEvent(Graphics2D g, Event event, int count) {
            int y = getYBandPosition(count);
            int x = timeStampToPosition(event.getTimeStamp());
            paintEvent(g, event.getDescription(), x, y);
        }

        private int getYBandPosition(int step) {
            // TODO can we do better in determining the number of 'bands' ?
            int TOTAL_STEPS = 10;
            step = step % TOTAL_STEPS + 1;

            return Math.round(1.0f * step * getHeight() / TOTAL_STEPS);
        }

        private void paintEvent(Graphics2D g, String text, int x, int y) {
            g = (Graphics2D) g.create();

            FontMetrics metrics = g.getFontMetrics();
            int descent = metrics.getDescent();
            int stringWidth = (int) Math.round(metrics.getStringBounds(text, g).getWidth());

            g.setPaint(eventTimeline.getEventPaint());
            g.drawLine(x, getHeight(), x, y + descent);
            g.drawLine(x, y + descent, x + stringWidth, y + descent);
            g.drawString(text, x, y);

            g.dispose();
        }

        @Override
        public int getSelectionMargin() {
            return MOUSE_MARGIN;
        }

        @Override
        public int getLeftSelectionPosition() {
            return left;
        }

        @Override
        public int getRightSelectionPosition() {
            return right;
        }

        @Override
        public void updateSelectionPosition(int newLeft, int newRight) {
            left = newLeft;
            right = newRight;
            Range<Long> range = new Range<>(positionToTimeStamp(left), positionToTimeStamp(right));
            eventTimeline.getModel().setDetailRange(range);
            refresh();
        }
    }

}

