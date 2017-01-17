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

package com.redhat.thermostat.vm.memory.client.core;

import java.util.List;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.memory.client.locale.LocaleResources;

public abstract class MemoryStatsView extends BasicView implements UIComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    //FIXME: Duplicate enum : See VmCpuView
    public enum UserAction {
        USER_CHANGED_TIME_RANGE,
    }

    public enum Type {
        TOTAL_ALLOCATING_THREADS(LocaleResources.TLAB_TOTAL_ALLOCATING_THREADS),
        TOTAL_ALLOCATIONS(LocaleResources.TLAB_TOTAL_ALLOCATIONS),
        TOTAL_REFILLS(LocaleResources.TLAB_TOTAL_REFILLS),
        MAX_REFILLS(LocaleResources.TLAB_MAX_REFILLS),
        TOTAL_SLOW_ALLOCATIONS(LocaleResources.TLAB_TOTAL_SLOW_ALLOCATIONS),
        MAX_SLOW_ALLOCATIONS(LocaleResources.TLAB_MAX_SLOW_ALLOCATIONS),
        TOTAL_GC_WASTE(LocaleResources.TLAB_TOTAL_GC_WASTE),
        MAX_GC_WASTE(LocaleResources.TLAB_MAX_GC_WASTE),
        TOTAL_SLOW_WASTE(LocaleResources.TLAB_TOTAL_SLOW_WASTE),
        MAX_SLOW_WASTE(LocaleResources.TLAB_MAX_SLOW_WASTE),
        TOTAL_FAST_WASTE(LocaleResources.TLAB_TOTAL_FAST_WASTE),
        MAX_FAST_WASTE(LocaleResources.TLAB_MAX_FAST_WASTE),

        ;

        private final LocalizedString label;

        Type(LocaleResources resource) {
            this.label = t.localize(resource);
        }

        public LocalizedString getLabel() {
            return this.label;
        }
    }
    
    public abstract void addRegion(Payload region);
    public abstract void updateRegion(Payload region);
    
    public abstract void setEnableGCAction(boolean enable);
    public abstract void addGCActionListener(ActionListener<GCAction> listener);

    public abstract void addUserActionListener(ActionListener<UserAction> actionListener);

    public abstract void requestRepaint();

    public abstract void displayWarning(LocalizedString string);

    public abstract void addTlabData(Type type, List<DiscreteTimeData<? extends Number>> data);
    public abstract void clearTlabData(Type type);

}

