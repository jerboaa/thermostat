/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.client.core.ToggleActionState;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.model.ThreadSession;

import java.util.List;

public abstract class ThreadView extends BasicView implements UIComponent {

    public enum ThreadAction {
        START_LIVE_RECORDING,
        STOP_LIVE_RECORDING,
        REQUEST_DISPLAY_RECORDED_SESSIONS,
        REQUEST_LOAD_SESSION,
    };

    public enum MonitoringState implements ToggleActionState {
        STARTED(true, false, true),
        STOPPED(true, false, false),
        STARTING(true, true, true),
        STOPPING(true, true, false),
        DISABLED(false, false, false),
        ;

        private final boolean isTransitionState;
        private final boolean isActionEnabled;
        private final boolean isButtonEnabled;

        MonitoringState(boolean isButtonEnabled, boolean isTransitionState, boolean isActionEnabled) {
            this.isButtonEnabled = isButtonEnabled;
            this.isTransitionState = isTransitionState;
            this.isActionEnabled = isActionEnabled;
        }

        @Override
        public boolean isTransitionState() {
            return isTransitionState;
        }

        @Override
        public boolean isActionEnabled() {
            return isActionEnabled;
        }

        @Override
        public boolean isButtonEnabled() {
            return isButtonEnabled;
        }
    }
    
    protected ApplicationService appService;
    protected String uniqueId;
    
    protected final ActionNotifier<ThreadAction> notifier;
    public ThreadView() {
        notifier = new ActionNotifier<>(this);
    }
    
    public void addThreadActionListener(ActionListener<ThreadAction> listener) {
        notifier.addActionListener(listener);
    }
    
    public void removeThreadActionListener(ActionListener<ThreadAction> listener) {
        notifier.removeActionListener(listener);
    }
    
    public abstract void setEnableRecordingControl(boolean enable);
    public abstract void setRecording(MonitoringState monitoringState, boolean notify);
    
    public abstract ThreadTableView createThreadTableView();
    public abstract ThreadTimelineView createThreadTimelineView();
    public abstract ThreadCountView createThreadCountView();
    public abstract LockView createLockView();
    public abstract StackTraceProfilerView createStackTraceProfilerView();

    public abstract void displayWarning(LocalizedString warning);

    public void setApplicationService(ApplicationService appService, String uniqueId) {
        this.appService = appService;
        this.uniqueId = uniqueId;
    }

    public abstract void displayThreadDetails(ThreadTableBean thread);

    public abstract VmDeadLockView createDeadLockView();
    public abstract void displayTimelineSessionList(List<ThreadSession> threadSessions);

}

