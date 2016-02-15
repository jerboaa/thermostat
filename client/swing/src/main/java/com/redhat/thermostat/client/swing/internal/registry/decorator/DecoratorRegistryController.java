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

package com.redhat.thermostat.client.swing.internal.registry.decorator;

import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorListener;
import com.redhat.thermostat.client.ui.Decorator;
import com.redhat.thermostat.client.ui.ToggleableReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import org.osgi.framework.InvalidSyntaxException;

import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorManager;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class DecoratorRegistryController {

    private DecoratorRegistryFactory registryFactory;
    private DecoratorManager decoratorManager;

    private InfoLabelDecoratorRegistry infoLabel;
    private MainLabelDecoratorRegistry mainLabel;
    private IconDecoratorRegistry iconDecorator;

    private ActionListener<ThermostatExtensionRegistry.Action> toggleableFilterListener;

    public DecoratorRegistryController(DecoratorRegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    public void init(HostTreeController hostController) {
        this.decoratorManager = hostController.getDecoratorManager();
        
        try {
            infoLabel = registryFactory.createInfoLabelDecoratorRegistry();
            mainLabel = registryFactory.createMainLabelDecoratorRegistry();
            iconDecorator = registryFactory.createIconDecoratorRegistry();
            
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void start() {
        ToggleStatusListener toggleStatusListener =
                new ToggleStatusListener(Arrays.asList(decoratorManager.getInfoLabelDecoratorListener(),
                decoratorManager.getMainLabelDecoratorListener(), decoratorManager.getIconDecoratorListener()));

        toggleableFilterListener = new ToggleableDecoratorFilterListener(toggleStatusListener);

        infoLabel.addActionListener(decoratorManager.getInfoLabelDecoratorListener());
        infoLabel.addActionListener(toggleableFilterListener);
        infoLabel.start();
        
        mainLabel.addActionListener(decoratorManager.getMainLabelDecoratorListener());
        mainLabel.addActionListener(toggleableFilterListener);
        mainLabel.start();
        
        iconDecorator.addActionListener(decoratorManager.getIconDecoratorListener());
        iconDecorator.addActionListener(toggleableFilterListener);
        iconDecorator.start();
    }

    public void stop() {
        infoLabel.removeActionListener(decoratorManager.getInfoLabelDecoratorListener());
        infoLabel.removeActionListener(toggleableFilterListener);
        infoLabel.stop();
        
        mainLabel.removeActionListener(decoratorManager.getMainLabelDecoratorListener());
        mainLabel.removeActionListener(toggleableFilterListener);
        mainLabel.stop();
        
        iconDecorator.removeActionListener(decoratorManager.getIconDecoratorListener());
        iconDecorator.removeActionListener(toggleableFilterListener);
        iconDecorator.stop();
    }

    ActionListener<ThermostatExtensionRegistry.Action> getToggleableFilterListener() {
        return toggleableFilterListener;
    }

    static class ToggleableDecoratorFilterListener implements ActionListener<ThermostatExtensionRegistry.Action> {

        private final ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> listener;

        ToggleableDecoratorFilterListener(ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void actionPerformed(ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
            if (!(actionEvent.getPayload() instanceof ToggleableReferenceFieldLabelDecorator)) {
                return;
            }
            ToggleableReferenceFieldLabelDecorator toggleable = (ToggleableReferenceFieldLabelDecorator) actionEvent.getPayload();
            switch (actionEvent.getActionId()) {
                case SERVICE_ADDED:
                    toggleable.addStatusEventListener(listener);
                    break;
                case SERVICE_REMOVED:
                    toggleable.removeStatusEventListener(listener);
                    break;
                default:
                    break;
            }
        }
    }

    static class ToggleStatusListener implements ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> {

        private final HashSet<DecoratorListener<? extends Decorator>> listeners = new HashSet<>();

        ToggleStatusListener(Collection<DecoratorListener<? extends Decorator>> listeners) {
            this.listeners.addAll(listeners);
        }

        @Override
        public void actionPerformed(ActionEvent<ToggleableReferenceFieldLabelDecorator.StatusEvent> actionEvent) {
            for (DecoratorListener<? extends Decorator> listener : listeners) {
                listener.fireDecorationChanged();
            }
        }
    }

}

