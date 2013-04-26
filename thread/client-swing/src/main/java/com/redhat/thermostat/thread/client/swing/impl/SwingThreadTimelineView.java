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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.BorderLayout;
import java.awt.Component;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;

import com.redhat.thermostat.client.swing.components.timeline.TimelineRulerHeader;

import com.redhat.thermostat.common.model.LongRange;

import com.redhat.thermostat.thread.client.common.Timeline;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;

import com.redhat.thermostat.thread.client.swing.impl.timeline.ThreadTimelineHeader;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineCellRenderer;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineComponent;

public class SwingThreadTimelineView extends ThreadTimelineView implements SwingComponent  {
        
    private JPanel timeLinePanel;
    private JList<TimelineComponent> chartList;
    private DefaultListModel<TimelineComponent> chartModel;
    
    private TimelineRulerHeader header;
    private JScrollPane scrollPane;
    
    public SwingThreadTimelineView() {
        timeLinePanel = new JPanel();
        timeLinePanel.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                SwingThreadTimelineView.this.notify(Action.VISIBLE);
            }
            
            @Override
            public void componentHidden(Component component) {
                SwingThreadTimelineView.this.notify(Action.HIDDEN);
            }
        });
        
        timeLinePanel.setLayout(new BorderLayout(0, 0));
        
        chartModel = new DefaultListModel<>();
        chartList = new JList<>(chartModel);
        
        chartList.setCellRenderer(new TimelineCellRenderer());
        
        JScrollPane scrollPane = createScrollPane();
        
        timeLinePanel.add(scrollPane, BorderLayout.CENTER);
        ThreadTimelineLegendPanel timelineLegend = new ThreadTimelineLegendPanel();
        timeLinePanel.add(timelineLegend, BorderLayout.SOUTH);
    }
    
    private class ScrollChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            scrollPane.repaint();
            header.repaint();
        }
    }
    
    private JScrollPane createScrollPane() {
        scrollPane = new JScrollPane(chartList);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        long now = System.currentTimeMillis();
        header = new ThreadTimelineHeader(new LongRange(now, now + TimelineRulerHeader.DEFAULT_INCREMENT_IN_MILLIS),
                                          scrollPane);
        scrollPane.setColumnHeaderView(header);

        ScrollChangeListener listener = new ScrollChangeListener();
        
        scrollPane.getHorizontalScrollBar().getModel().addChangeListener(listener);
        scrollPane.getVerticalScrollBar().getModel().addChangeListener(listener);
        
        return scrollPane;
    }
    
    private void handleScrollBar() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {        
                JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();                
                if (!chartModel.isEmpty()) {
                    TimelineComponent component = chartModel.getElementAt(0);

                    int extent = scrollBar.getVisibleAmount();
                    int min = scrollBar.getMinimum();
                    int max = component.getWidth() + (int) (2 * header.getUnitIncrementInMillis());

                    scrollBar.setValues(max - extent, extent, min, max);
                }
            }
        });
    }
    
    @Override
    public void displayStats(final List<Timeline> timelines, final LongRange range) {
                
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                range.setMax(range.getMax() + (int) (2 * header.getUnitIncrementInMillis()));
                chartModel.removeAllElements();
                for (Timeline timeline : timelines) {
                    
                    TimelineComponent timelineComp = new TimelineComponent(range, timeline, scrollPane);
                    timelineComp.setUnitIncrementInMillis(header.getUnitIncrementInMillis());
                    timelineComp.setUnitIncrementInPixels(header.getUnitIncrementInPixels());
                    
                    chartModel.addElement(timelineComp);
                }
                header.getRange().setMin(range.getMin());
                header.getRange().setMax(range.getMax());
                
                handleScrollBar();
            }
        });
    }
    
    @Override
    public Component getUiComponent() {
        return timeLinePanel;
    }
}

