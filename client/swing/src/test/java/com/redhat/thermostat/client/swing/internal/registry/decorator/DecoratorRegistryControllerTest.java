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

package com.redhat.thermostat.client.swing.internal.registry.decorator;

import static org.hamcrest.core.IsAnything.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.redhat.thermostat.client.ui.Decorator;
import com.redhat.thermostat.client.ui.ToggleableReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorManager;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorListener;

import java.util.Collections;

/**
 *
 */
public class DecoratorRegistryControllerTest {

    private DecoratorRegistryFactory registryFactory;
    private HostTreeController hostController;

    private DecoratorListener l0;
    private DecoratorListener l1;
    private DecoratorListener l2;

    private InfoLabelDecoratorRegistry infoLabel;
    private MainLabelDecoratorRegistry mainLabel;
    private IconDecoratorRegistry icon;

    private DecoratorManager manager;
    
    @Before
    public void setUp() throws InvalidSyntaxException {
        
        manager = mock(DecoratorManager.class);
        
        infoLabel = mock(InfoLabelDecoratorRegistry.class);
        mainLabel = mock(MainLabelDecoratorRegistry.class);
        icon = mock(IconDecoratorRegistry.class);

        registryFactory = mock(DecoratorRegistryFactory.class);
        
        when(registryFactory.createInfoLabelDecoratorRegistry()).thenReturn(infoLabel);
        when(registryFactory.createMainLabelDecoratorRegistry()).thenReturn(mainLabel);
        when(registryFactory.createIconDecoratorRegistry()).thenReturn(icon);

        l0 = mock(DecoratorListener.class);
        l1 = mock(DecoratorListener.class);
        l2 = mock(DecoratorListener.class);

        hostController = mock(HostTreeController.class);
        when(hostController.getDecoratorManager()).thenReturn(manager);
        
        when(manager.getInfoLabelDecoratorListener()).thenReturn(l0);
        when(manager.getMainLabelDecoratorListener()).thenReturn(l1);
        when(manager.getIconDecoratorListener()).thenReturn(l2);
    }
    
    @Test
    public void testInit() throws InvalidSyntaxException {
        DecoratorRegistryController controller =
                new DecoratorRegistryController(registryFactory);
        controller.init(hostController);
        
        verify(registryFactory).createInfoLabelDecoratorRegistry();
        verify(registryFactory).createMainLabelDecoratorRegistry();
        verify(registryFactory).createIconDecoratorRegistry();

    }

    @Test
    public void testStart() throws InvalidSyntaxException {
        DecoratorRegistryController controller =
                new DecoratorRegistryController(registryFactory);
        controller.init(hostController);
        
        controller.start();
        
        verify(infoLabel).addActionListener(l0);
        verify(infoLabel).addActionListener(controller.getToggleableFilterListener());
        verify(infoLabel).start();
        
        verify(mainLabel).addActionListener(l1);
        verify(mainLabel).addActionListener(controller.getToggleableFilterListener());
        verify(mainLabel).start();
        
        verify(mainLabel).addActionListener(l1);
        verify(mainLabel).addActionListener(controller.getToggleableFilterListener());
        verify(mainLabel).start();
        
        verify(mainLabel).addActionListener(l1);
        verify(mainLabel).addActionListener(controller.getToggleableFilterListener());
        verify(mainLabel).start();
        
        verify(icon).addActionListener(l2);
        verify(icon).addActionListener(controller.getToggleableFilterListener());
        verify(icon).start();
    }

    @Test
    public void testStop() throws InvalidSyntaxException {
        DecoratorRegistryController controller =
                new DecoratorRegistryController(registryFactory);
        controller.init(hostController);

        // since it's all mocked, we don't need to start anything
        // controller.start();

        controller.stop();

        verify(infoLabel).removeActionListener(l0);
        verify(infoLabel).removeActionListener(controller.getToggleableFilterListener());
        verify(infoLabel).stop();

        verify(mainLabel).removeActionListener(l1);
        verify(mainLabel).removeActionListener(controller.getToggleableFilterListener());
        verify(mainLabel).stop();

        verify(mainLabel).removeActionListener(l1);
        verify(mainLabel).removeActionListener(controller.getToggleableFilterListener());
        verify(mainLabel).stop();

        verify(mainLabel).removeActionListener(l1);
        verify(mainLabel).removeActionListener(controller.getToggleableFilterListener());
        verify(mainLabel).stop();

        verify(icon).removeActionListener(l2);
        verify(icon).removeActionListener(controller.getToggleableFilterListener());
        verify(icon).stop();
    }

    @Test @SuppressWarnings("unchecked")
    public void testToggleableDecoratorFilterListenerOnServiceAdded() {
        ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> statusEventActionListener =
                (ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent>) mock(ActionListener.class);

        ToggleableReferenceFieldLabelDecorator decorator = mock(ToggleableReferenceFieldLabelDecorator.class);

        ActionEvent<ThermostatExtensionRegistry.Action> event =
                new ActionEvent<>(decorator, ThermostatExtensionRegistry.Action.SERVICE_ADDED);
        event.setPayload(decorator);

        DecoratorRegistryController.ToggleableDecoratorFilterListener filterListener =
                new DecoratorRegistryController.ToggleableDecoratorFilterListener(statusEventActionListener);

        filterListener.actionPerformed(event);
        verify(decorator).addStatusEventListener(statusEventActionListener);
    }

    @Test @SuppressWarnings("unchecked")
    public void testToggleableDecoratorFilterListenerOnServiceRemoved() {
        ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> statusEventActionListener =
                (ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent>) mock(ActionListener.class);

        ToggleableReferenceFieldLabelDecorator decorator = mock(ToggleableReferenceFieldLabelDecorator.class);

        ActionEvent<ThermostatExtensionRegistry.Action> event =
                new ActionEvent<>(decorator, ThermostatExtensionRegistry.Action.SERVICE_REMOVED);
        event.setPayload(decorator);

        DecoratorRegistryController.ToggleableDecoratorFilterListener filterListener =
                new DecoratorRegistryController.ToggleableDecoratorFilterListener(statusEventActionListener);

        filterListener.actionPerformed(event);
        verify(decorator).removeStatusEventListener(statusEventActionListener);
    }

    @Test @SuppressWarnings("unchecked")
    public void testToggleStatusListener() {
        DecoratorListener<ToggleableReferenceFieldLabelDecorator> decoratorListener =
                (DecoratorListener<ToggleableReferenceFieldLabelDecorator>) mock(DecoratorListener.class);

        DecoratorRegistryController.ToggleStatusListener toggleStatusListener =
                new DecoratorRegistryController.ToggleStatusListener(
                        Collections.<DecoratorListener<? extends Decorator>>singleton(decoratorListener)
                );

        ActionEvent<ToggleableReferenceFieldLabelDecorator.StatusEvent> event =
                new ActionEvent<>(decoratorListener, ToggleableReferenceFieldLabelDecorator.StatusEvent.STATUS_CHANGED);
        event.setPayload(decoratorListener);

        toggleStatusListener.actionPerformed(event);

        verify(decoratorListener).fireDecorationChanged();
    }

}

