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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.setup.command.internal.cli.CharArrayMatcher;

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
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, structureInfo, mock(AuthFileWriter.class), mock(KeyringWriter.class));
        when(structureInfo.isWebAppInstalled()).thenReturn(true);
        assertTrue(setup.isWebAppInstalled());
        verify(structureInfo).isWebAppInstalled();
    }
    
    @Test
    public void testCreateAgentUser() {
        AuthFileWriter writer = mock(AuthFileWriter.class);
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), writer, mock(KeyringWriter.class));
        setup.createAgentUser("foo-agent", new char[] { 't' });
        verify(userSetup).createRecursiveRole(eq("thermostat-agent"), argThat(new RoleMatcher(UserRoles.AGENT_ROLES)), any(String.class));
        verify(userSetup).assignRolesToUser(eq("foo-agent"), argThat(new RoleMatcher(new String[] { "thermostat-agent", UserRoles.GRANT_FILES_WRITE_ALL })), any(String.class));
        verify(writer).setCredentials(eq("foo-agent"), argThat(matchesPassword(new char[] { 't' })));
    }
    
    private CharArrayMatcher matchesPassword(char[] password) {
        return new CharArrayMatcher(password);
    }
    
    @Test
    public void testCreateClientAdminUser() {
        KeyringWriter writer = mock(KeyringWriter.class);
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), mock(AuthFileWriter.class), writer);
        setup.createClientAdminUser("foo-client", new char[] { 't' });
        verify(userSetup).createRecursiveRole(eq("thermostat-client"), argThat(new RoleMatcher(UserRoles.CLIENT_ROLES)), any(String.class));
        verify(userSetup).createRecursiveRole(eq("thermostat-cmdc"), argThat(new RoleMatcher(UserRoles.CMD_CHANNEL_GRANT_ALL_ACTIONS)), any(String.class));
        verify(userSetup).createRecursiveRole(eq("thermostat-admin-read-all"), argThat(new RoleMatcher(UserRoles.ADMIN_READALL)), any(String.class));
        String[] clientAllRoles = new String[] { "thermostat-client", "thermostat-cmdc", "thermostat-admin-read-all", UserRoles.PURGE };
        verify(userSetup).assignRolesToUser(eq("foo-client"), argThat(new RoleMatcher(clientAllRoles)), any(String.class));
        verify(writer).setCredentials(eq("foo-client"), argThat(matchesPassword(new char[] { 't' })));
    }
    
    @Test
    public void commitCreatesAgentAuthFileStoresToKeyringWhenWebappInstalled() throws IOException {
        StructureInformation info = mock(StructureInformation.class);
        when(info.isWebAppInstalled()).thenReturn(true);
        AuthFileWriter authWriter = mock(AuthFileWriter.class);
        KeyringWriter keyringWriter = mock(KeyringWriter.class);
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, info, authWriter, keyringWriter);
        setup.commit();
        verify(authWriter).write();
        verify(keyringWriter).write();
        verify(userSetup).commit();
        verify(mongoUserSetup).commit();
    }
    
    @Test
    public void commitOnlyCommitsMongodbCredsWhenWebappIsNotInstalled() throws IOException {
        StructureInformation info = mock(StructureInformation.class);
        when(info.isWebAppInstalled()).thenReturn(false);
        AuthFileWriter authWriter = mock(AuthFileWriter.class);
        KeyringWriter keyringWriter = mock(KeyringWriter.class);
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, info, authWriter, keyringWriter);
        setup.commit();
        verify(mongoUserSetup).commit();
        verifyNoMoreInteractions(authWriter);
        verifyNoMoreInteractions(keyringWriter);
        verifyNoMoreInteractions(userSetup);
    }
    
    @Test
    public void testDetermineReasonFromExceptionStorageRunningException() {
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), mock(AuthFileWriter.class), mock(KeyringWriter.class));
        Throwable testException = mock(Throwable.class);
        when(testException.getCause()).thenReturn(mock(MongodbUserSetup.StorageAlreadyRunningException.class));
        String reason = setup.determineReasonFromException(testException);
        assertTrue(reason.equals("Thermostat storage is already running. Please stop storage and then run setup again."));
    }

    @Test
    public void testDetermineReasonFromExceptionGenericException() {
        ThermostatSetup setup = new ThermostatSetup(userSetup, mongoUserSetup, mock(StructureInformation.class), mock(AuthFileWriter.class), mock(KeyringWriter.class));
        Throwable testException = mock(Throwable.class);
        when(testException.getCause()).thenReturn(mock(Exception.class));
        when(testException.getLocalizedMessage()).thenReturn("test error message");
        String reason = setup.determineReasonFromException(testException);
        assertTrue(reason.equals("test error message"));
    }
    
    private static class RoleMatcher extends BaseMatcher<String[]> {
        
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
