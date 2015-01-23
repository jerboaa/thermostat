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

package com.redhat.thermostat.client.swing.internal;

import javax.swing.JFrame;

import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.swing.internal.search.ReferenceFieldSearchFilter;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.Ref;

public interface MainView {

    enum Action {
        VISIBLE,
        HIDDEN,
        SHOW_AGENT_CONFIG,
        SHOW_CLIENT_CONFIG,
        SHOW_ABOUT_DIALOG,
        SHUTDOWN,
    }

    void addActionListener(ActionListener<Action> capture);
    
    void setWindowTitle(String title);

    void showMainWindow();

    void hideMainWindow();

    void setSubView(BasicView view);

    void setStatusBarPrimaryStatus(LocalizedString primaryStatus);
    
    /**
     * Adds a menu item to the window. Assumes the menu path is valid (has a
     * non-zero length) and doesn't collide with existing menus.
     */
    void addMenu(MenuAction action);

    /**
     * Returns the progress notifier associate with this view.
     */
    ProgressNotifier getNotifier();
    
    /**
     * Removes a menu item to the window. Assumes the menu path is valid (has a
     * non-zero length) and the menu already exists.
     */
    void removeMenu(MenuAction action);

    JFrame getTopFrame();

    /**
     * Returns the {@link HostTreeController} that handles the {@link Ref}s
     * object tracked by this UI Client.
     */
    HostTreeController getHostTreeController();

    /**
     * Returns the {@link ContextActionController} that handles the context
     * actions in the UI Client.
     */
    ContextActionController getContextActionController();

    /**
     * Returns the filter used for searching references inside the reference
     * tree.
     */
    ReferenceFieldSearchFilter getSearchFilter();
}

