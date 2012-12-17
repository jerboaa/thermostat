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

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.thread.client.common.ThreadTimelineBean;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class SwingThreadTimelineView extends ThreadTimelineView  implements SwingComponent  {

    private final String lock = new String("SwingThreadTimelineViewLock");
        
    private JPanel timeLinePanel;
    private JList<SwingThreadTimelineChart> chartList;
    private DefaultListModel<SwingThreadTimelineChart> chartModel;
    
    private String leftMarkerMessage; 
    private String rightMarkerMessage;
    
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
        
        chartList.setCellRenderer(new ChartRenderer());
        chartList.addMouseListener(new ChartListListener());
        
        JScrollPane scrollPane = new JScrollPane(chartList);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        timeLinePanel.add(scrollPane, BorderLayout.CENTER);
        ThreadTimelineLegendPanel timelineLegend = new ThreadTimelineLegendPanel();
        timeLinePanel.add(timelineLegend, BorderLayout.SOUTH);
    }
    
    @Override
    public void displayStats(final Map<ThreadInfoData, List<ThreadTimelineBean>> timelines, final long start, final long stop) {

        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                String _leftMarkerMessage = null; 
                String _rightMarkerMessage = null;
                synchronized (lock) {
                    _leftMarkerMessage = leftMarkerMessage;
                    _rightMarkerMessage = rightMarkerMessage;                        
                }
                
                chartModel.clear();
                for (List<ThreadTimelineBean> timeline : timelines.values()) {
                    SwingThreadTimelineChart panel = new SwingThreadTimelineChart(timeline, start, stop);
                    panel.setPreferredSize(new Dimension(chartList.getWidth(), 75));
                    panel.setMarkersMessage(_leftMarkerMessage, _rightMarkerMessage);
                    panel.addPropertyChangeListener(SwingThreadTimelineChart.HIGHLIGHT_THREAD_STATE_PROPERTY,
                                                    new SelectedThreadListener());
                    panel.setMarkersMessage(_leftMarkerMessage, _rightMarkerMessage);
                    chartModel.addElement(panel);
                }
            }
        });
    }
    
    private class SelectedThreadListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            SwingWorker<Void, Void> notifier = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SwingThreadTimelineView.this.
                    threadTimelineNotifier.fireAction(ThreadTimelineView.ThreadTimelineViewAction.THREAD_TIMELINE_SELECTED,
                                                      evt.getNewValue());
                    return null;
                }
            };
            notifier.execute();
        }
    }
    
    private class ChartRenderer implements ListCellRenderer<SwingThreadTimelineChart> {
        @Override
        public Component getListCellRendererComponent(JList<? extends SwingThreadTimelineChart> list,
                                                      SwingThreadTimelineChart chart,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus)
        {
            if (!isSelected) {
                chart.unsetHighlightArea();
            }
            return chart;
        }
    }
    
    private class ChartListListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            int index = chartList.getSelectedIndex();
            if (index > 0) {
                Point listIndex = chartList.indexToLocation(index);
                Point absoluteLocation = e.getPoint();
                listIndex.x = absoluteLocation.x;
                if (index != 0) {
                    listIndex.y = absoluteLocation.y - listIndex.y;
                }
                
                SwingThreadTimelineChart chart = chartModel.get(index);
                chart.clickAndHighlightArea(listIndex);                
            }
        }
    }
    
    @Override
    public Component getUiComponent() {
        return timeLinePanel;
    }
    
    @Override
    public void resetMarkerMessage() {
        synchronized (lock) {
            this.leftMarkerMessage = null;
            this.rightMarkerMessage = null;            
        }
    }
    
    @Override
    public void setMarkersMessage(String left, String right) {
        synchronized (lock) {
            this.leftMarkerMessage = left;
            this.rightMarkerMessage = right;            
        }
    }
}
