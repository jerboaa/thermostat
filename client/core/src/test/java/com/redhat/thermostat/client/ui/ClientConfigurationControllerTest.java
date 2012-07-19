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

package com.redhat.thermostat.client.ui;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.config.ClientPreferences;

public class ClientConfigurationControllerTest {

    @Test
    public void verifyShowDialog() {
        ClientPreferences model = mock(ClientPreferences.class);
        when(model.getConnectionUrl()).thenReturn("mock-connection-url");
        when(model.getPassword()).thenReturn("mock-password");
        when(model.getUserName()).thenReturn("mock-username");
        
        ClientConfigurationView view = mock(ClientConfigurationView.class);
        ClientConfigurationController controller = new ClientConfigurationController(model, view);

        controller.showDialog();

        verify(model).getConnectionUrl();
        verify(view).setConnectionUrl(eq("mock-connection-url"));
        verify(view).setPassword(eq("mock-password"));
        verify(view).setUserName(eq("mock-username"));
        verify(view).showDialog();
    }

    @Test
    public void verifyCloseCancel() {
        ClientPreferences model = mock(ClientPreferences.class);
        ClientConfigurationView view = mock(ClientConfigurationView.class);
        ClientConfigurationController controller = new ClientConfigurationController(model, view);

        controller.actionPerformed(new ActionEvent<>(view, ClientConfigurationView.Action.CLOSE_CANCEL));

        verify(model, times(0)).setConnectionUrl(any(String.class));
        verify(model, times(0)).setPassword(any(String.class));
        verify(model, times(0)).setUserName(any(String.class));
        
        verify(view, times(0)).getConnectionUrl();
        verify(view, times(0)).showDialog();
        verify(view).hideDialog();
    }

    @Test
    public void verifyCloseAccept() {
        ClientPreferences model = mock(ClientPreferences.class);
        ClientConfigurationView view = mock(ClientConfigurationView.class);
        when(view.getConnectionUrl()).thenReturn("mock-connection-url");
        when(view.getPassword()).thenReturn("mock-password");
        when(view.getUserName()).thenReturn("mock-username");
        
        ClientConfigurationController controller = new ClientConfigurationController(model, view);

        controller.actionPerformed(new ActionEvent<>(view, ClientConfigurationView.Action.CLOSE_ACCEPT));

        verify(model).setConnectionUrl(eq("mock-connection-url"));
        verify(model).setPassword(eq("mock-password"));
        verify(model).setUserName(eq("mock-username"));
        
        verify(view).getConnectionUrl();
        verify(view, times(0)).showDialog();
        verify(view).hideDialog();
    }

}
