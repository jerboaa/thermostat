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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.ui.DecoratorProvider;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.storage.core.Ref;

/*
 * Provide an implementation of a listener for Registry update. This listener
 * is only to be used to listen for DecoratorProvider changes and its sole
 * purpouse is to re-route the generic VmRef or HostRef DecoratorProvider
 * changes to the actual Decoration Manager.
 */
public class DecoratorProviderExtensionListener<T extends Ref> implements ActionListener<ThermostatExtensionRegistry.Action>
{   
    public enum Action {
        DECORATOR_ADDED,
        DECORATOR_REMOVED,
        DECORATION_CHANGED,
    }
    
    private final ActionNotifier<Action> decoratorChangeNotifier;

    private Logger logger = Logger.getLogger(DecoratorProviderExtensionListener.class.getSimpleName());    

    private CopyOnWriteArrayList<DecoratorProvider<T>> decorators;

    public DecoratorProviderExtensionListener() {
        this.decoratorChangeNotifier = new ActionNotifier<>(this);
        decorators = new CopyOnWriteArrayList<>();
    }

    public void addDecoratorChangeListener(ActionListener<Action> listener) {
        decoratorChangeNotifier.addActionListener(listener);
    }
    
    public void removeDecoratorChangeListener(ActionListener<Action> listener) {
        decoratorChangeNotifier.removeActionListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed(ActionEvent<com.redhat.thermostat.common.
                                ThermostatExtensionRegistry.Action> actionEvent)
    {
        Object payload = actionEvent.getPayload();
        if (! (payload instanceof DecoratorProvider)) {
            throw new IllegalArgumentException("unexpected payload type. " +
                                               payload.getClass().getName() +
                                               "not allowed here.");
        }

        decorators.add((DecoratorProvider<T>) payload);
        
        switch (actionEvent.getActionId()) {
        case SERVICE_ADDED:
            decoratorChangeNotifier.fireAction(Action.DECORATOR_ADDED, payload);
            break;

        case SERVICE_REMOVED:
            decoratorChangeNotifier.fireAction(Action.DECORATOR_REMOVED, payload);
            break;

        default:
            logger.log(Level.WARNING, "received unknown event from ExtensionRegistry: " +
                                       actionEvent.getActionId());
            break;
        }
    }
    
    CopyOnWriteArrayList<DecoratorProvider<T>> getDecorators() {
        return decorators;
    }

    public void decorationChanged() {
        decoratorChangeNotifier.fireAction(Action.DECORATION_CHANGED);
    }
}
