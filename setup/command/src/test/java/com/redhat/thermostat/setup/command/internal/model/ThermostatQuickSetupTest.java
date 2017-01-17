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

package com.redhat.thermostat.setup.command.internal.model;

import com.redhat.thermostat.setup.command.internal.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThermostatQuickSetupTest {
    private ThermostatSetup thermostatSetup;

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    @Before
    public void setup() {
        thermostatSetup = mock(ThermostatSetup.class);
    }

    @Test
    public void testAgentUsername() {
        when(thermostatSetup.isWebAppInstalled()).thenReturn(true);
        ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);
        String username = quickSetup.getAgentUsername();
        Assert.assertTrue(username.startsWith(translator.localize(LocaleResources.AGENT_USER_PREFIX).getContents()));
    }

    @Test
    public void testAgentUsernameWebAppNotInstalled() {
        when(thermostatSetup.isWebAppInstalled()).thenReturn(false);
        ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);
        String username = quickSetup.getAgentUsername();
        Assert.assertTrue(username.startsWith(translator.localize(LocaleResources.USER_PREFIX).getContents()));
    }

    @Test
    public void testClientUsername() {
        when(thermostatSetup.isWebAppInstalled()).thenReturn(true);
        ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);
        String username = quickSetup.getClientUsername();
        Assert.assertTrue(username.startsWith(translator.localize(LocaleResources.CLIENT_USER_PREFIX).getContents()));
    }

    @Test
    public void testClientUsernameWebAppNotInstalled() {
        when(thermostatSetup.isWebAppInstalled()).thenReturn(false);
        ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);
        String username = quickSetup.getClientUsername();
        Assert.assertTrue(username.startsWith(translator.localize(LocaleResources.USER_PREFIX).getContents()));
    }

    @Test
    public void testRun() throws IOException {
        when(thermostatSetup.isWebAppInstalled()).thenReturn(true);
        ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);

        quickSetup.run();

        verify(thermostatSetup, times(1)).createMongodbUser(anyString(), any(char[].class));
        verify(thermostatSetup, times(1)).createAgentUser(anyString(), any(char[].class));
        verify(thermostatSetup, times(1)).createClientAdminUser(anyString(), any(char[].class));
        verify(thermostatSetup, times(1)).commit();
    }


    @Test
    public void testRunWebAppNotInstalled() throws IOException {
        when(thermostatSetup.isWebAppInstalled()).thenReturn(false);
        ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);

        quickSetup.run();

        verify(thermostatSetup, times(1)).createMongodbUser(anyString(), any(char[].class));
        verify(thermostatSetup, times(0)).createAgentUser(anyString(), any(char[].class));
        verify(thermostatSetup, times(0)).createClientAdminUser(anyString(), any(char[].class));
        verify(thermostatSetup, times(1)).commit();
    }

}
