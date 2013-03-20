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

package com.redhat.thermostat.client.swing.internal;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JFrame;

import com.redhat.thermostat.client.core.Filter;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.ui.ContextAction;
import com.redhat.thermostat.client.ui.DecoratorProvider;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.HostsVMsLoader;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;

public interface MainView {

    enum Action {
        VISIBLE,
        HIDDEN,
        HOST_VM_TREE_FILTER,
        HOST_VM_SELECTION_CHANGED,
        SHOW_AGENT_CONFIG,
        SHOW_CLIENT_CONFIG,
        SWITCH_HISTORY_MODE,
        SHOW_ABOUT_DIALOG,
        SHUTDOWN,
        SHOW_HOST_VM_CONTEXT_MENU,
        HOST_VM_CONTEXT_ACTION,
    }

    void addActionListener(ActionListener<Action> capture);

    void updateTree(List<Filter<HostRef>> hostFilters, List<Filter<VmRef>> vmFilters,
            List<DecoratorProvider<HostRef>> hostDecorators, List<DecoratorProvider<VmRef>> vmDecorators,
            HostsVMsLoader any);

    String getHostVmTreeFilterText();
    
    void setWindowTitle(String title);

    void showMainWindow();

    void hideMainWindow();

    Ref getSelectedHostOrVm();

    void setSubView(BasicView view);

    void setStatusBarPrimaryStatus(String primaryStatus);
    
    /**
     * Adds a menu item to the window. Assumes the menu path is valid (has a
     * non-zero length) and doesn't collide with existing menus.
     */
    void addMenu(MenuAction action);

    /**
     * Removes a menu item to the window. Assumes the menu path is valid (has a
     * non-zero length) and the menu already exists.
     */
    void removeMenu(MenuAction action);

    /**
     * Shows a popup context menu created from the list of supplied context
     * actions. When an item in the popup menu is selected, an
     * {@link ActionEvent} is fired with the id
     * {@link Action#HOST_VM_CONTEXT_ACTION} and the user-selected
     * {@link ContextAction} as the payload.
     *
     * @param actions the {@link ContextAction}s available to the user.
     * Normally classes implementing sub-interfaces of {@link ContextAction} are used here.
     * @param e the mouse event that triggered the context action. Used to
     * position the context menu.
     */
    void showContextActions(List<ContextAction> actions, MouseEvent e);
    
    JFrame getTopFrame();
}

