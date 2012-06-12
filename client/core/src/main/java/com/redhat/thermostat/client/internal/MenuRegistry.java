/*
 * Copyright 2012 Red Hat, Inc.
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
package com.redhat.thermostat.client.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.osgi.service.MenuAction;

public class MenuRegistry {

    public static interface MenuListener {

        public void added(String parentMenuName, MenuAction action);

        public void removed(String parentMenuName, MenuAction action);
    }

    public static final String PARENT_MENU = "parentMenu";

    private static final String FILTER = "(&(" + Constants.OBJECTCLASS + "=" + MenuAction.class.getName() + ")(" + PARENT_MENU + "=*))";

    private ServiceTracker menuTracker;

    private Map<String,List<MenuAction>> menus = new HashMap<>();
    private List<MenuListener> listeners = new CopyOnWriteArrayList<>();

    public MenuRegistry(BundleContext context) throws InvalidSyntaxException {
        menuTracker = new ServiceTracker(context, FrameworkUtil.createFilter(FILTER), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                MenuAction action = (MenuAction) super.addingService(reference);
                String parentMenuName = (String) reference.getProperty(PARENT_MENU);
                menuAdded(parentMenuName, action);
                return action;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                if (!(service instanceof MenuAction)) {
                    throw new AssertionError("removing a non-MenuAction service");
                }
                String parentMenuName = (String) reference.getProperty(PARENT_MENU);
                menuRemoved(parentMenuName, (MenuAction)service);
                super.removedService(reference, service);
            }
        };
    }

    public void start() {
        menuTracker.open();
    }

    public void stop() {
        menuTracker.close();
    }

    public void addMenuListener(MenuListener listener) {
        listeners.add(listener);

        for (Entry<String,List<MenuAction>> entry: menus.entrySet()) {
            for (MenuAction action: entry.getValue()) {
                listener.added(entry.getKey(), action);
            }
        }
    }

    public void removeMenuListener(MenuListener listener) {
        listeners.remove(listener);
    }

    private void menuAdded(String parentMenuName, MenuAction action) {
        if (!menus.containsKey(parentMenuName)) {
            menus.put(parentMenuName, new ArrayList<MenuAction>());
        }
        List<MenuAction> list = menus.get(parentMenuName);
        list.add(action);
        for (MenuListener listener: listeners) {
            listener.added(parentMenuName, action);
        }
    }

    private void menuRemoved(String parentMenuName, MenuAction action) {
        if (!menus.containsKey(parentMenuName)) {
            throw new IllegalArgumentException("unknown parent menu name");
        }
        List<MenuAction> list = menus.get(parentMenuName);
        if (!list.contains(action)) {
            throw new IllegalArgumentException("unknown menu action");
        }

        list.remove(action);
        for (MenuListener listener: listeners) {
            listener.removed(parentMenuName, action);
        }
    }

}
