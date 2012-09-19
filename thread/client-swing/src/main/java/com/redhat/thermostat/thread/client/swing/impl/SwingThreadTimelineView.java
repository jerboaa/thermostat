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
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.client.ui.SwingComponent;
import com.redhat.thermostat.thread.client.common.ThreadTimelineBean;
import com.redhat.thermostat.thread.client.common.ThreadTimelineView;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class SwingThreadTimelineView extends ThreadTimelineView  implements SwingComponent  {

    private JPanel timeLinePanel;
    private DefaultListModel<SwingThreadTimelineChart> model;
    
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
        model = new DefaultListModel<>();
        JList<SwingThreadTimelineChart> chartList = new JList<>(model);
        
        chartList.setLayoutOrientation(JList.VERTICAL);
        chartList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(chartList);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        timeLinePanel.add(scrollPane);
        chartList.setCellRenderer(new ListCellRenderer<SwingThreadTimelineChart>() {
            @Override
            public Component getListCellRendererComponent(
                    JList<? extends SwingThreadTimelineChart> list, SwingThreadTimelineChart value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                return value;
            }
        });
    }
    
    @Override
    public void displayStats(final Map<ThreadInfoData, List<ThreadTimelineBean>> timelines, final long start, final long stop) {

        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                model.clear();
                for (List<ThreadTimelineBean> timeline : timelines.values()) {
                    SwingThreadTimelineChart panel = new SwingThreadTimelineChart(timeline, start, stop);
                    panel.setPreferredSize(new Dimension(timeLinePanel.getWidth(), 50));
                    model.addElement(panel);
                }
            }
        });
    }
    
    @Override
    public Component getUiComponent() {
        return timeLinePanel;
    }
}
