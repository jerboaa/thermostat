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

package com.redhat.thermostat.client.filter.vm.core.internal;

import com.redhat.thermostat.client.filter.vm.core.LivingHostFilter;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

public class LivingHostFilterMenuAction implements MenuAction {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private LivingHostFilter filter;
    
    public LivingHostFilterMenuAction(LivingHostFilter filter) {
        this.filter = filter;
    }
    
    @Override
    public LocalizedString getName() {
        return t.localize(LocaleResources.SHOW_DEAD_HOST_NAME);
    }

    @Override
    public LocalizedString getDescription() {
        return t.localize(LocaleResources.SHOW_DEAD_HOST_DESC);
    }

    @Override
    public void execute() {
        filter.setActive(!filter.isActive());
    }

    @Override
    public Type getType() {
        return Type.CHECK;
    }

    @Override
    public LocalizedString[] getPath() {
        return new LocalizedString[] { t.localize(LocaleResources.VIEW_MENU), getName() };
    }

    @Override
    public int sortOrder() {
        return SORT_TOP;
    }

}

