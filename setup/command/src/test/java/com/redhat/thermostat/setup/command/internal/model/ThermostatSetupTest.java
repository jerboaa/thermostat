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

package com.redhat.thermostat.setup.command.internal.model;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ThermostatSetupTest {

    private ThermostatUserSetup userSetup;
    private MongodbUserSetup mongoUserSetup;
    
    @Before
    public void setup() {
        userSetup = mock(ThermostatUserSetup.class);
        mongoUserSetup = mock(MongodbUserSetup.class);
    }
    
    @Test
    public void testIsWebAppInstalledDelegates() {
        StructureInformation structureInfo = mock(StructureInformation.class);
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, structureInfo, mock(CommonPaths.class), mock(CredentialsFileCreator.class), mock(Keyring.class), mock(ClientPreferences.class));
        when(structureInfo.isWebAppInstalled()).thenReturn(true);
        assertTrue(setup.isWebAppInstalled());
        verify(structureInfo).isWebAppInstalled();
    }
    
    @Test
    public void testCreateAgentUser() {
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), mock(CommonPaths.class), mock(CredentialsFileCreator.class), mock(Keyring.class), mock(ClientPreferences.class));
        setup.createAgentUser("foo-agent", new char[] { 't' });
        verify(userSetup).createRecursiveRole(eq("thermostat-agent"), argThat(new RoleMatcher(UserRoles.AGENT_ROLES)), any(String.class));
        verify(userSetup).assignRolesToUser(eq("foo-agent"), argThat(new RoleMatcher(new String[] { "thermostat-agent", UserRoles.GRANT_FILES_WRITE_ALL })), any(String.class));
    }
    
    @Test
    public void testCreateClientAdminUser() {
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), mock(CommonPaths.class), mock(CredentialsFileCreator.class), mock(Keyring.class), mock(ClientPreferences.class));
        setup.createClientAdminUser("foo-client", new char[] { 't' });
        verify(userSetup).createRecursiveRole(eq("thermostat-client"), argThat(new RoleMatcher(UserRoles.CLIENT_ROLES)), any(String.class));
        verify(userSetup).createRecursiveRole(eq("thermostat-cmdc"), argThat(new RoleMatcher(UserRoles.CMD_CHANNEL_GRANT_ALL_ACTIONS)), any(String.class));
        verify(userSetup).createRecursiveRole(eq("thermostat-admin-read-all"), argThat(new RoleMatcher(UserRoles.ADMIN_READALL)), any(String.class));
        String[] clientAllRoles = new String[] { "thermostat-client", "thermostat-cmdc", "thermostat-admin-read-all", UserRoles.PURGE };
        verify(userSetup).assignRolesToUser(eq("foo-client"), argThat(new RoleMatcher(clientAllRoles)), any(String.class));
    }
    
    @Test
    public void commitCreatesAgentAuthFileStoresToKeyring() throws IOException {
        CommonPaths paths = mock(CommonPaths.class);
        File mockAgentAuthFile = File.createTempFile("thermostat-test-", getClass().getName());
        Keyring keyring = mock(Keyring.class);
        ClientPreferences prefs = mock(ClientPreferences.class);
        try {
            when(paths.getUserAgentAuthConfigFile()).thenReturn(mockAgentAuthFile);
            ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), paths, mock(CredentialsFileCreator.class), keyring, prefs);
            List<String> contents = Files.readAllLines(mockAgentAuthFile.toPath(), Charset.forName("UTF-8"));
            assertEquals(0, contents.size());
            setup.createAgentUser("damian", new char[] { 't', 'e', 's', 't' });
            String clientUser = "client-admin";
            char[] clientPass = new char[] { 't' };
            setup.createClientAdminUser(clientUser, clientPass);
            setup.commit();
            verify(userSetup).commit();
            verify(mongoUserSetup).commit();
            verify(keyring).savePassword(prefs.getConnectionUrl(), clientUser, clientPass);
            verify(prefs).flush();
            verify(prefs).setSaveEntitlements(true);
            verify(prefs).setUserName(clientUser);
            contents = Files.readAllLines(mockAgentAuthFile.toPath(), Charset.forName("UTF-8"));
            assertTrue("username and password must be present", contents.size() > 2);
            assertTrue("username=damian expected to be found in agent.auth file", contents.contains("username=damian"));
            assertTrue("password=test expected to be found in agent.auth file", contents.contains("password=test"));
        } finally {
            Files.delete(mockAgentAuthFile.toPath());
        }
    }
    
    private class RoleMatcher extends BaseMatcher<String[]> {
        
        final String[] expected;
        private RoleMatcher(String[] expected) {
            this.expected = expected;
        }

        @Override
        public void describeTo(Description arg0) {
            arg0.appendText(Arrays.asList(expected).toString());
        }

        @Override
        public boolean matches(Object arg0) {
            if (arg0.getClass() != String[].class) {
                return false;
            }
            String[] other = (String[])arg0;
            if (other.length != expected.length) {
                return false;
            }
            boolean match = true;
            for (int i = 0; i < expected.length; i++) {
                match = match && Objects.equals(expected[i], other[i]);
            }
            return match;
        }
        
    }
}
