/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import com.redhat.thermostat.annotations.ExtensionPoint;
import com.redhat.thermostat.client.core.Filter;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.VmRef;

/**
 * {@code VMContextAction}s provide actions that are associated with Java
 * Virtual Machines and can be invoked by users. The exact position and
 * appearance of these {@code VMContextAction}s varies based on the client
 * implementation.
 * <p>
 * Plugins can register implementation of this interface as OSGi services to
 * provide additional {@code VMContextAction}s.
 * <p>
 * <h2>Implementation Note</h2>
 * <p>
 * The following information is specific to the current release and may change
 * in a future release.
 * <p>
 * The swing client uses instances of this class to provide menu items in the
 * Host/VM tree. The menu is shown when a user right-clicks a VM in the Host/VM
 * tree. A menu item for every {@link VMContextAction} is added, if the
 * {@code Filter} matches, to this menu. Selecting a menu item invokes the
 * corresponding {@code VMContextAction}.
 *
 * @see HostContextAction
 */
@ExtensionPoint
public interface VMContextAction extends ContextAction {

    /**
     * A user-visible name for this {@code VMContextAction}. Should be
     * localized.
     */
    @Override
    public LocalizedString getName();

    /**
     * A user-visible description for {@code VMContextAction}. Should be
     * localized.
     */
    @Override
    public LocalizedString getDescription();

    /**
     * Invoked when the user selects this {@code VMContextAction}.
     *
     * @param reference specifies the vm that this {@code VMContextAction} was
     * invoked on.
     */
    void execute(VmRef reference);

    /**
     * The {@link Filter} returned by this method is used to select what VMs
     * this {@code VMContextAction} is applicable to.
     */
    Filter<VmRef> getFilter();
}

