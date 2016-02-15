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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import java.awt.Component;
import java.util.List;
import java.util.Objects;

import com.redhat.thermostat.client.core.ToggleActionState;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;

public abstract class VmProfileView extends BasicView implements UIComponent {

    static class Profile {
        public final String name;
        public final long startTimeStamp;
        public final long stopTimeStamp;
        public Profile(String name, long startTimeStamp, long stopTimeStamp) {
            this.name = name;
            this.startTimeStamp = startTimeStamp;
            this.stopTimeStamp = stopTimeStamp;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != Profile.class) {
                return false;
            }
            Profile other = (Profile) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.startTimeStamp, other.startTimeStamp)
                    && Objects.equals(this.stopTimeStamp, other.stopTimeStamp);
        }

        @Override
        public String toString() {
            return "Profile (" + this.name + ", [" + this.startTimeStamp + "," + this.stopTimeStamp + "] )";
        }

    }

    enum TabbedPaneAction {
        TABLE_TAB_SELECTED,
        TREEMAP_TAB_SELECTED,
    }

    enum ProfileAction {
        START_PROFILING,
        STOP_PROFILING,

        PROFILE_SELECTED,
    }

    enum ProfilingState implements ToggleActionState {
        STARTED(true, false, true),
        STOPPED(true, false, false),
        STARTING(true, true, true),
        STOPPING(true, true, false),
        DISABLED(false, false, false),
        ;

        private final boolean isTransitionState;
        private final boolean isActionEnabled;
        private final boolean isButtonEnabled;

        ProfilingState(boolean isButtonEnabled, boolean isTransitionState, boolean isActionEnabled) {
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

    public abstract void addProfileActionListener(ActionListener<ProfileAction> listener);

    public abstract void removeProfileActionlistener(ActionListener<ProfileAction> listener);

    public abstract void setTabbedPaneActionListener(ActionListener<TabbedPaneAction> listener);

    /*
     * Because of the latency between asking for starting profiling and actually
     * starting profiling, we use a lot more states than 'enabled/disabled' for
     * indicating profiling in the UI
     */

    public abstract void setProfilingState(ProfilingState profilingState);
    public abstract void setViewControlsEnabled(boolean enabled);

    public abstract void setAvailableProfilingRuns(List<Profile> data);

    public abstract Profile getSelectedProfile();

    public abstract void setProfilingDetailData(ProfilingResult results);

    public abstract void addTabToTabbedPane(final LocalizedString title, final Component component);

}
