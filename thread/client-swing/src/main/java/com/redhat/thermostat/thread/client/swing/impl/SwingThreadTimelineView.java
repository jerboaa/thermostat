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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.common.model.LongRange;

import com.redhat.thermostat.thread.client.common.Timeline;
import com.redhat.thermostat.thread.client.common.TimelineInfo;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineCellRenderer;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineComponent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineRulerHeader;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineUtils;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class SwingThreadTimelineView extends ThreadTimelineView implements SwingComponent  {

    private final String lock = new String("SwingThreadTimelineViewLock");
        
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
    
    private JScrollPane createScrollPane() {
        scrollPane = new JScrollPane(chartList);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        long now = System.currentTimeMillis();
        header = new TimelineRulerHeader(new LongRange(now, now + TimelineUtils.STEP), scrollPane);
        scrollPane.setColumnHeaderView(header);
        scrollPane.getHorizontalScrollBar().getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                scrollPane.repaint();
            }
        });
        scrollPane.getVerticalScrollBar().getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                scrollPane.repaint();
            }
        });        
        return scrollPane;
    }
    
    @Override
    public void displayStats(final List<Timeline> timelines, final LongRange range) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chartModel.removeAllElements();
                for (Timeline timeline : timelines) {
                    chartModel.addElement(new TimelineComponent(range, timeline, scrollPane));
                }
                header.getRange().setMin(range.getMin());
                header.getRange().setMax(range.getMax());
            }
        });
    }
    
//    private class SelectedThreadListener implements PropertyChangeListener {
//        @Override
//        public void propertyChange(final PropertyChangeEvent evt) {
//            SwingWorker<Void, Void> notifier = new SwingWorker<Void, Void>() {
//                @Override
//                protected Void doInBackground() throws Exception {
//                    SwingThreadTimelineView.this.
//                    threadTimelineNotifier.fireAction(ThreadTimelineView.ThreadTimelineViewAction.THREAD_TIMELINE_SELECTED,
//                                                      evt.getNewValue());
//                    return null;
//                }
//            };
//            notifier.execute();
//        }
//    }
    
    @Override
    public Component getUiComponent() {
        return timeLinePanel;
    }
}
