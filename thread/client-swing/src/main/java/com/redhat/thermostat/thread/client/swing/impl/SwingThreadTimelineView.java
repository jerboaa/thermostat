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

package com.redhat.thermostat.thread.client.swing.impl;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineProbe;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.swing.experimental.utils.EDTHelper;
import com.redhat.thermostat.thread.client.swing.impl.timeline.RangeComponent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineComponent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineContainer;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineViewComponent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RangedTimelineProbe;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineDateFormatter;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

public class SwingThreadTimelineView extends ThreadTimelineView implements SwingComponent  {

    private final UIDefaults uiDefaults;
    private TimelineViewComponent contentPane;

    private ComponentVisibilityNotifier visibilityNotifier;

    private Map<ThreadInfo, TimelineComponent> timelines;

    private EDTHelper edt;

    public SwingThreadTimelineView(UIDefaults uiDefaults) {
        this.uiDefaults = uiDefaults;

        edt = new EDTHelper();
        timelines = new HashMap<>();

        this.contentPane = new TimelineViewComponent(uiDefaults);
        clear();

        visibilityNotifier = new ComponentVisibilityNotifier();
        visibilityNotifier.initialize(contentPane, notifier);
    }

    @Override
    public void addThread(final ThreadInfo thread) {
        edt.callLater(new Runnable() {
            @Override
            public void run() {
                if (!timelines.containsKey(thread)) {
                    TimelineComponent timeline =
                            new TimelineComponent(uiDefaults, thread,
                                                  contentPane.getModel());
                    timeline.initComponents();

                    timelines.put(thread, timeline);
                    contentPane.addTimeline(timeline);
                }
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return contentPane;
    }

    @Override
    public void setTotalRange(final Range<Long> totalRange) {
        edt.callLater(new Runnable() {
            @Override
            public void run() {
                contentPane.getModel().setRange(totalRange);
            }
        });
    }

    @Override
    public void clear() {
        edt.callLater(new Runnable() {
            @Override
            public void run() {
                timelines.clear();
                contentPane.removeAll();
                contentPane.initComponents();
                contentPane.revalidate();
                contentPane.repaint();
            }
        });
    }

    @Override
    public void addProbe(final ThreadInfo info, final TimelineProbe state) {
        edt.callLater(new Runnable() {
            @Override
            public void run() {
                TimelineComponent component = timelines.get(info);
                TimelineContainer timelineContainer =
                        component.getTimelineContainer();
                RangeComponent rangeComponent =
                        timelineContainer.getLastRangeComponent();

                if (rangeComponent == null) {
                    setRangedComponent(state, timelineContainer);

                } else {
                    RangedTimelineProbe probe = rangeComponent.getInfo();
                    probe.setProbeEnd(state.getTimeStamp());
                    if (!probe.getColor().equals(state.getColor())) {
                        setRangedComponent(state, timelineContainer);
                    }
                }
                timelineContainer.revalidate();
                timelineContainer.repaint();
            }
        });
    }

    private void setRangedComponent(TimelineProbe state,
                                    TimelineContainer timelineContainer)
    {
        RangedTimelineProbe probe =
                new RangedTimelineProbe(state, state.getTimeStamp());
        RangeComponent rangeComponent = new RangeComponent(probe);
        rangeComponent.setToolTipText(state.getState() + " - " +
                                      TimelineDateFormatter.format(state.
                                              getTimeStamp()));
        timelineContainer.add(rangeComponent);
    }
}

