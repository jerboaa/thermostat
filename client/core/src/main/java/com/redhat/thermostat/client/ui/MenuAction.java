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
package com.redhat.thermostat.client.ui;

import com.redhat.thermostat.annotations.ExtensionPoint;
import com.redhat.thermostat.shared.locale.LocalizedString;

/**
 * {@code MenuAction}s are used to create top-level menu items in the main
 * window.
 * <p>
 * Plugins can register menu items by creating classes that implement this
 * interface and registering them as OSGi services. To register a menu item for
 * for the menu "File" in thermostat client window, register a service that
 * returns <code> {"File", getName()}</code> from {@link #getPath()}.
 *
 * <h2>Implementation Notes</h2>
 * <p>
 * The following information is specific to the current release and may change
 * in a future release.
 * <p>
 * The swing client uses {@code MenuActions}s to implement top-level menus in
 * the main window only.
 */
@ExtensionPoint
public interface MenuAction extends ContextAction {

    public static enum Type {
        CHECK,
        RADIO,
        STANDARD
    }

    /** The user-visible text displayed as the menu item. */
    @Override
    public LocalizedString getName();

    /** A user-visible description of what this {@code MenuAction} does. */
    @Override
    public LocalizedString getDescription();

    /** Invoked when the user selects this menu item */
    void execute();

    /** The type of the menu (radio, check, standard) */
    Type getType();

    /** The path to the menu action. The last element must equal getName() */
    LocalizedString[] getPath();

}

