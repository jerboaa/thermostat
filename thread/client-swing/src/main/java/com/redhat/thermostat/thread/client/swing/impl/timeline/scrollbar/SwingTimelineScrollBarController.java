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

package com.redhat.thermostat.thread.client.swing.impl.timeline.scrollbar;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineGroupDataModel;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.swing.impl.SwingThreadTimelineView;
import com.redhat.thermostat.thread.client.swing.impl.timeline.SwingTimelineDimensionModel;
import com.redhat.thermostat.thread.client.swing.impl.timeline.TimelineGroupThreadConverter;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class SwingTimelineScrollBarController implements PropertyChangeListener, AdjustmentListener {

    private static final int MIN_THUMB_PERCENTAGE = 2;

    private boolean followMode;

    private TimelineScrollBar scrollbar;
    private TimelineGroupThreadConverter groupDataModel;
    private SwingTimelineDimensionModel dimensionModel;
    private SwingThreadTimelineView view;

    public SwingTimelineScrollBarController(SwingThreadTimelineView view,
                                            TimelineScrollBar scrollbar,
                                            TimelineGroupThreadConverter groupDataModel,
                                            SwingTimelineDimensionModel dimensionModel)
    {
        this.scrollbar = scrollbar;
        this.groupDataModel = groupDataModel;
        this.dimensionModel = dimensionModel;
        this.view = view;
    }

    public void initScrollbar(SwingThreadTimelineView threadTimelineView) {
        scrollbar.setEnabled(false);
        scrollbar.addAdjustmentListener(this);
        groupDataModel.addPropertyChangeListener(TimelineGroupDataModel.RangeChangeProperty.TOTAL_RANGE, this);
        groupDataModel.addPropertyChangeListener(TimelineGroupDataModel.RangeChangeProperty.PAGE_RANGE, this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (scrollbar.getValueIsAdjusting()) {
            // we won't do anything at this point
            return;
        }

        Range<Long> pageRange = groupDataModel.getDataModel().getPageRange();
        Range<Long> totalRange = groupDataModel.getDataModel().getTotalRange();

        double pageSize = (double) (pageRange.getMax() - pageRange.getMin());
        double totalSize = (double) (totalRange.getMax() - totalRange.getMin());
        int width = dimensionModel.getWidth();

        // no data, or not everything has been instantiated correctly yet
        if (totalSize <= 0 || width <= 0) {
            return;
        }

        // this happens when the whole data is less than the page size
        if (totalSize <= pageSize) {
            // set the whole thing 100% extent, the slider should
            // not be enabled
            scrollbar.setEnabled(false);
            scrollbar.setValues(0, 100, 0, 100);

        } else {
            // ensure the scrollbar is enabled
            scrollbar.setEnabled(true);

            int amount = (int) Math.round((pageSize/totalSize) * 100);
            if (amount < MIN_THUMB_PERCENTAGE) {
                amount = MIN_THUMB_PERCENTAGE;
            }

            int value = scrollbar.getValue();
            int max = scrollbar.getMaximum();
            if (followMode || (value + amount) >= max) {
                value = max - amount;
            }
            //            scrollbar.setValues(max - amount, amount, 0, 100);
            scrollbar.setValues(value, amount, 0, 100);
        }
    }

    public void ensureTimelineState(ThreadTimelineView.TimelineSelectorState following) {
        switch (following) {
            default:
            case FOLLOWING: {

                followMode = true;

                int amount = scrollbar.getVisibleAmount();
                int max = scrollbar.getMaximum();
                int total = max + amount;
                int currentValue = scrollbar.getValue() + amount;
                if (currentValue <= total) {
                    scrollbar.setValue(total);
                }

            }   break;

            case STATIC:

                followMode = false;

                // there is really nothing else to do here, the scrollbar stays
                // where it is, this is taken care by the range handlers
                break;

        }
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        Object source = e.getSource();
        if (source instanceof TimelineScrollBar) {
            TimelineScrollBar scrollBar = (TimelineScrollBar) source;
            int value = scrollBar.getValue();
            int visible = scrollBar.getVisibleAmount();
            int max = scrollBar.getMaximum();

            int currentExtent = value + visible;
            if (!scrollBar.getValueIsAdjusting() && currentExtent == max) {
                view.requestFollowMode();
            } else {

                Range<Long> totalRange = groupDataModel.getDataModel().getTotalRange();

                long totalSize = totalRange.getMax() - totalRange.getMin();
                long length = dimensionModel.getLengthInMillis();

                long currentStep = Math.round((value * totalSize) / 100);

                // what's the actual range based on current percentage on
                // display?
                long start = totalRange.getMin() + currentStep;
                Range<Long> pageRange = new Range<>(start, start + length);
                view.requestStaticMode(pageRange);
            }
        }
    }
}
