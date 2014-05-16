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

package com.redhat.thermostat.thread.client.common.view;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.model.timeline.Timeline;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineGroupDataModel;
import com.redhat.thermostat.thread.model.ThreadHeader;
import java.util.List;

public abstract class ThreadTimelineView extends BasicView {

    public static enum ThreadTimelineViewAction {
        THREAD_TIMELINE_SELECTED,
        SWITCH_TO_FOLLOW_MODE,
        SWITCH_TO_STATIC_MODE,
    }

    public static enum TimelineSelectorState {
        FOLLOWING,
        STATIC,
    }

    protected final ActionNotifier<ThreadTimelineViewAction> threadTimelineNotifier;
    
    public ThreadTimelineView() {
        threadTimelineNotifier = new ActionNotifier<>(this);
    }
    
    public void addThreadSelectionActionListener(ActionListener<ThreadTimelineViewAction> listener) {
        threadTimelineNotifier.addActionListener(listener);
    }
    
    public void removeThreadSelectionActionListener(ActionListener<ThreadTimelineViewAction> listener) {
        threadTimelineNotifier.removeActionListener(listener);
    }

    protected void requestFollowMode() {
        threadTimelineNotifier.fireAction(ThreadTimelineViewAction.SWITCH_TO_FOLLOW_MODE);
    }

    protected void requestStaticMode(Range<Long> pageRange) {
        threadTimelineNotifier.fireAction(ThreadTimelineViewAction.SWITCH_TO_STATIC_MODE, pageRange);
    }

    /**
     * Returns the {@link TimelineGroupDataModel} for this view.
     */
    public abstract TimelineGroupDataModel getGroupDataModel();

    /**
     * Ensures that the Timeline selector is in one of the possible states.
     */
    public abstract void ensureTimelineState(TimelineSelectorState following);

    /**
     * Update the list of all {@link ThreadHeader} currently displayed by
     * this view.
     */
    public abstract void updateThreadList(List<ThreadHeader> threads);

    /**
     * Display the given {@link Timeline} associated to this {@link ThreadHeader}.
     */
    public abstract void displayTimeline(ThreadHeader thread, Timeline threadTimeline);

    /**
     * Notify the View that all the changes may be delivered to the rendering
     * thread.
     */
    public abstract void submitChanges();
}

