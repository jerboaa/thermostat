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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.core.views.ClientConfigurationView;
import com.redhat.thermostat.client.core.views.ClientConfigurationView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.utils.keyring.KeyringException;

public class ClientConfigurationControllerTest {

    private ClientPreferencesModel model;
    private ClientConfigurationView view;

    @Before
    public void setUp() {
        model = mock(ClientPreferencesModel.class);
        when(model.getConnectionUrl()).thenReturn("mock-connection-url");
        when(model.getUserName()).thenReturn("mock-username");
        when(model.getPassword()).thenReturn("mock-password".toCharArray());
        when(model.getSaveEntitlements()).thenReturn(false);

        view = mock(ClientConfigurationView.class);
        when(view.getConnectionUrl()).thenReturn("mock-connection-url");
        when(view.getPassword()).thenReturn("mock-password".toCharArray());
        when(view.getUserName()).thenReturn("mock-username");
        when(view.getSaveEntitlements()).thenReturn(true);
    }

    public void tearDown() {
        view = null;
        model = null;
    }

    @Test
    public void verifyShowDialog() {
        
        ClientConfigurationController controller = new ClientConfigurationController(model, view);

        controller.showDialog();

        verify(model).getConnectionUrl();
        verify(view).setConnectionUrl(eq("mock-connection-url"));
        verify(view).setPassword(eq("mock-password".toCharArray()));
        verify(view).setUserName(eq("mock-username"));
        verify(view).setSaveEntitlements(eq(false));
        verify(view).showDialog();
    }

    @Test
    public void verifyCloseCancel() {
        ClientConfigurationController controller = new ClientConfigurationController(model, view);

        controller.actionPerformed(new ActionEvent<>(view, ClientConfigurationView.Action.CLOSE_CANCEL));

        verifyCloseCancelCommon();
    }

    @Test
    public void verifyCloseCancelWithReconnector() {
        ClientConfigReconnector reconnector = mock(ClientConfigReconnector.class);
        ClientConfigurationController controller = new ClientConfigurationController(model, view, reconnector);

        controller.actionPerformed(new ActionEvent<>(view, ClientConfigurationView.Action.CLOSE_CANCEL));

        verifyCloseCancelCommon();
        verify(reconnector).abort();
    }

    private void verifyCloseCancelCommon() {
        verify(model, times(0)).setConnectionUrl(any(String.class));
        verify(model, times(0)).setCredentials(any(String.class), isA(char[].class));
        
        verify(view, times(0)).getConnectionUrl();
        verify(view, times(0)).showDialog();
        verify(view).hideDialog();
    }

    @Test
    public void verifyCloseAccept() {
        
        ClientConfigurationController controller = new ClientConfigurationController(model, view);

        controller.actionPerformed(new ActionEvent<>(view, ClientConfigurationView.Action.CLOSE_ACCEPT));

        verifyCloseAcceptCommon();
    }

    @Test
    public void verifyCloseAcceptWithReconnector() {
        
        ClientConfigReconnector reconnector = mock(ClientConfigReconnector.class);
        ClientConfigurationController controller = new ClientConfigurationController(model, view, reconnector);

        controller.actionPerformed(new ActionEvent<>(view, ClientConfigurationView.Action.CLOSE_ACCEPT));

        verifyCloseAcceptCommon();
        verify(reconnector).reconnect(eq(model.getPreferences()), any(StorageCredentials.class));
    }

    private void verifyCloseAcceptCommon() {
        verify(model).setConnectionUrl(eq("mock-connection-url"));
        verify(model).setCredentials(any(String.class), isA(char[].class));
        verify(model).setSaveEntitlements(eq(true));
        
        verify(view).getConnectionUrl();
        verify(view, times(0)).showDialog();
        verify(view).hideDialog();
    }

    @Test
    public void verifyCatchesKeyringException() {
        ClientPreferencesModel badModel = mock(ClientPreferencesModel.class);
        doThrow(new KeyringException("")).when(badModel).setCredentials("mock-username", "mock-password".toCharArray());
        ClientConfigurationController controller = new ClientConfigurationController(badModel, view);
        ActionEvent event = mock(ActionEvent.class);
        when(event.getActionId()).thenReturn(Action.CLOSE_ACCEPT);
        try {
            controller.actionPerformed(event);
        } catch (KeyringException e) {
            e.printStackTrace();
            // Such an exception should be caught within the controller.
            fail();
        }
    }

}

