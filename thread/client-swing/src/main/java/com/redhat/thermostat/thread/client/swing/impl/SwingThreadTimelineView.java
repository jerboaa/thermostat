/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.model.timeline.Timeline;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineGroupDataModel;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.swing.impl.timeline.HeaderController;
import com.redhat.thermostat.thread.client.swing.impl.timeline.SwingTimelineDimensionModel;
import com.redhat.thermostat.thread.client.swing.impl.timeline.ThreadTimelineHeader;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineCellRenderer;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineComponent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineGroupThreadConverter;
import com.redhat.thermostat.thread.client.swing.impl.timeline.scrollbar.SwingTimelineScrollBarController;
import com.redhat.thermostat.thread.client.swing.impl.timeline.scrollbar.TimelineScrollBar;
import com.redhat.thermostat.thread.model.ThreadHeader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class SwingThreadTimelineView extends ThreadTimelineView implements SwingComponent  {

    private SwingTimelineScrollBarController scrollBarController;
    private Map<ThreadHeader, TimelineComponent> timelineMap;

    private TimelineGroupThreadConverter groupDataModel;
    private SwingTimelineDimensionModel dimensionModel;

    private DefaultListModel<TimelineComponent> timelineModel;
    private JList<TimelineComponent> timelines;

    private ThreadTimelineHeader header;
    private JPanel contentPane;
    private JScrollPane scrollPane;

    public SwingThreadTimelineView(UIDefaults uiDefaults,
                                   final SwingTimelineDimensionModel dimensionModel)
    {
        timelineMap = new HashMap<>();

        this.dimensionModel = dimensionModel;

        TimelineGroupDataModel realGDM = new TimelineGroupDataModel();
        groupDataModel = new TimelineGroupThreadConverter(realGDM);

        contentPane = new JPanel();
        contentPane.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                SwingThreadTimelineView.this.notify(Action.VISIBLE);

                // TODO: this should be retrieved from state properties
                requestFollowMode();
            }
            
            @Override
            public void componentHidden(Component component) {
                SwingThreadTimelineView.this.notify(Action.HIDDEN);
            }
        });

        contentPane.setLayout(new BorderLayout(0, 0));

        JPanel timelineBottomControls = new JPanel();
        timelineBottomControls.setLayout(new BorderLayout(0, 0));

        TimelineScrollBar scrollbar = setupTimelineScrollBar(uiDefaults);
        timelineBottomControls.add(scrollbar, BorderLayout.NORTH);

        createScrollPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);

        ThreadTimelineLegendPanel timelineLegend = new ThreadTimelineLegendPanel();
        timelineBottomControls.add(timelineLegend, BorderLayout.SOUTH);

        contentPane.add(timelineBottomControls, BorderLayout.SOUTH);
        contentPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
            dimensionModel.setWidth(contentPane.getWidth());
            }
        });
    }

    TimelineScrollBar setupTimelineScrollBar(UIDefaults uiDefaults) {

        TimelineScrollBar scrollbar = new TimelineScrollBar(uiDefaults);
        scrollBarController = new SwingTimelineScrollBarController(this, scrollbar,
                                                                   groupDataModel,
                                                                   dimensionModel);
        scrollBarController.initScrollbar(this);
        return scrollbar;
    }

    private void createScrollPane() {

        timelineModel = new DefaultListModel<>();
        timelines = new JList<>(timelineModel);
        timelines.setCellRenderer(new TimelineCellRenderer());
        timelines.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        scrollPane = new ThermostatScrollPane(timelines);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        header = new ThreadTimelineHeader(groupDataModel, dimensionModel);
        header.setName("TimelineRulerHeader_thread");

        scrollPane.setColumnHeaderView(header);
        groupDataModel.addPropertyChangeListener(TimelineGroupDataModel.RangeChangeProperty.PAGE_RANGE,
                                                 new HeaderController(header, timelines));
    }

    @Override
    public Component getUiComponent() {
        return contentPane;
    }

    @Override
    public void ensureTimelineState(final TimelineSelectorState following) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollBarController.ensureTimelineState(following);
            }
        });
    }
    
    @Override
    public void updateThreadList(final List<ThreadHeader> threads) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO: remove timelines that are not in the list, but
                // still present onscreen
                for (ThreadHeader thread : threads) {
                    if (!timelineMap.containsKey(thread)) {
                        TimelineComponent timeline =
                                new TimelineComponent(groupDataModel,
                                                      dimensionModel,
                                                      thread.getThreadName());
                        timelineMap.put(thread, timeline);
                        timelineModel.addElement(timeline);
                    }
                }
            }
        });
    }

    @Override
    public TimelineGroupDataModel getGroupDataModel() {
        return groupDataModel.getDataModel();
    }

    @Override
    public void displayTimeline(final ThreadHeader thread,
                                final Timeline threadTimeline)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimelineComponent timeline = timelineMap.get(thread);
                if (timeline != null) {
                    timeline.setTimeline(threadTimeline);
                }
            }
        });
    }
    @Override
    public void submitChanges() {
        contentPane.revalidate();
    }


    // rise visibility so other classes here can use those methods through us
    @Override
    public void requestFollowMode() {
        super.requestFollowMode();
    }

    @Override
    public void requestStaticMode(Range<Long> pageRange) {
        super.requestStaticMode(pageRange);
    }
}

