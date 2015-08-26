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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.setup.command.internal.model.ThermostatUserSetup.CommentedCredsPropertyValue;
import com.redhat.thermostat.setup.command.internal.model.ThermostatUserSetup.CommentedRolePropertyValue;

public class ThermostatUserSetupTest {
    
    private ThermostatUserSetup tSetup;
    
    @Before
    public void setup() throws IOException {
        tSetup = new ThermostatUserSetup(mock(UserPropertiesFinder.class), mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class));
    }
    
    @Test
    public void canCreateCommentedUser() {
        char[] fooUserPwd = new char[] { 't', 'e', 's', 't'};
        tSetup.createUser("foo-user", fooUserPwd, "some-comment");
        char[] otherBarPwd = new char[] { 'o', 't', 'h', 'e', 'r'};
        tSetup.createUser("other-bar", otherBarPwd, "comment for bar");
        char[] thirdUserPwd = new char[] { 'n', 'o', 'm', 'a', 't', 't', 'e', 'r' };
        tSetup.createUser("third-user", thirdUserPwd, null);
        Map<String, CommentedCredsPropertyValue> props = tSetup.buildUserProperties();
        
        CommentedCredsPropertyValue fooUser = props.get("foo-user");
        assertSame(fooUserPwd, fooUser.getValue());
        assertEquals("some-comment", fooUser.getComment());
        
        CommentedCredsPropertyValue otherBar = props.get("other-bar");
        assertSame(otherBarPwd, otherBar.getValue());
        assertEquals("comment for bar", otherBar.getComment());
        
        CommentedCredsPropertyValue thirdValue = props.get("third-user");
        assertSame(thirdUserPwd, thirdValue.getValue());
        assertNull(thirdValue.getComment());
        
        assertNull(props.get("i-wasn't-created-user"));
    }
    
    @Test
    public void canCreateCommentedRecursiveRole() {
        String[] primitives = new String[] {
                "role1",
                "role2"
        };
        tSetup.createRecursiveRole("recursive-role", primitives, "comment for recursive role");
        Map<String, CommentedRolePropertyValue> roles = tSetup.buildRoleProperties();
        CommentedRolePropertyValue recRoleValue = roles.get("recursive-role");
        assertNotNull(recRoleValue);
        String expectedValue = "role1, " + System.lineSeparator() +
                               "role2";
        assertEquals(expectedValue, recRoleValue.getValue());
        assertEquals("comment for recursive role", recRoleValue.getComment());
    }
    
    @Test
    public void canAssignRolesToUser() {
        String[] primitives = new String[] {
                "role1",
                "role2"
        };
        tSetup.assignRolesToUser("some-user", primitives, null);
        Map<String, CommentedRolePropertyValue> roleProps = tSetup.buildRoleProperties();
        CommentedRolePropertyValue val = roleProps.get("some-user");
        assertNotNull(val);
        assertNull(val.getComment());
        String expectedRolesStr = "role1, " + System.lineSeparator() + "role2";
        assertEquals(expectedRolesStr, val.getValue());
    }
    
    @Test
    public void testRoleSetToString() {
        String[] roleSet = new String[] {
                "foo-one",
                "foo-two"
        };
        String expectedString = "foo-one, " + System.lineSeparator() + "foo-two";
        assertEquals(expectedString, tSetup.roleSetToString(roleSet));
        assertTrue(tSetup.roleSetToString(new String[] {}).isEmpty());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void assigningRolesValidatesNullValue() {
        tSetup.assignRolesToUser("something", null, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void assigningRolesValidatesEmptyRoles() {
        tSetup.assignRolesToUser("something", new String[] {}, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void creatingRecursiveRolesValidatesNullValue() {
        tSetup.createRecursiveRole("something", null, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void creatingRecursiveRolesValidatesEmptyRoles() {
        tSetup.createRecursiveRole("something", new String[] {}, null);
    }
    
    @Test
    public void canWriteRoles() throws IOException {
        Map<String, CommentedRolePropertyValue> roleProps = new HashMap<>();
        String roleValue = "thermostat-login, \nthermostat-test";
        CommentedRolePropertyValue thermostatAgent = new CommentedRolePropertyValue(roleValue, "some comment");
        roleProps.put("thermostat-agent", thermostatAgent);
        CommentedRolePropertyValue agentUser = new CommentedRolePropertyValue("thermostat-agent", "agent user");
        roleProps.put("foo-agent-user", agentUser);
        
        UserPropertiesFinder propsFinder = mock(UserPropertiesFinder.class);
        File tmpRolesPropsFile = File.createTempFile("thermostat", getClass().getName());
        try {
            when(propsFinder.getRolesProperties()).thenReturn(tmpRolesPropsFile);
            ThermostatUserSetup userSetup = new ThermostatUserSetup(propsFinder, mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class)) {
                
                @Override
                Properties readPropsFromFile(File propsFile) {
                    // simulate empty existing properties
                    return new Properties();
                }
            };
            userSetup.writeRoles(roleProps);
            List<String> expectedList = new ArrayList<>();
            expectedList.add("#some comment");
            expectedList.add("thermostat-agent=thermostat-login, \\");
            expectedList.add("foo-agent-user=thermostat-agent");
            expectedList.add("#agent user");
            verifyFileContainsLines(tmpRolesPropsFile, expectedList);
        } finally {
            Files.delete(tmpRolesPropsFile.toPath());
        }
    }
    
    @Test
    public void canWriteUsers() throws IOException {
        Map<String, CommentedCredsPropertyValue> userCreds = new HashMap<>();
        CommentedCredsPropertyValue user1Val = new CommentedCredsPropertyValue(new char[] { 't' }, "user1");
        userCreds.put("testuser1", user1Val);
        CommentedCredsPropertyValue user2Val = new CommentedCredsPropertyValue(new char[] { 'b', 'a', 'r' }, "speedy");
        userCreds.put("speedy-user", user2Val);
        
        UserPropertiesFinder propsFinder = mock(UserPropertiesFinder.class);
        File tmpUsersPropsFile = File.createTempFile("thermostat", getClass().getName());
        try {
            when(propsFinder.getUserProperties()).thenReturn(tmpUsersPropsFile);
            ThermostatUserSetup userSetup = new ThermostatUserSetup(propsFinder, mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class)) {
                
                @Override
                Properties readPropsFromFile(File propsFile) {
                    return new Properties();
                }
            };
            userSetup.writeUsers(userCreds);
            List<String> expectedList = new ArrayList<>();
            expectedList.add("#user1");
            expectedList.add("testuser1=t");
            expectedList.add("speedy-user=bar");
            expectedList.add("#speedy");
            verifyFileContainsLines(tmpUsersPropsFile, expectedList);
        } finally {
            Files.delete(tmpUsersPropsFile.toPath());
        }
    }
    
    @Test
    public void writingUsersDoesnotProduceDuplicates() throws IOException {
        Map<String, CommentedCredsPropertyValue> userCreds = new HashMap<>();
        CommentedCredsPropertyValue user1Val = new CommentedCredsPropertyValue(new char[] { 't' }, "user1");
        userCreds.put("testuser1", user1Val);
        CommentedCredsPropertyValue user2Val = new CommentedCredsPropertyValue(new char[] { 'b', 'a', 'r' }, "speedy");
        userCreds.put("speedy-user", user2Val);
        
        UserPropertiesFinder propsFinder = mock(UserPropertiesFinder.class);
        File tmpUsersPropsFile = File.createTempFile("thermostat", getClass().getName());
        try {
            when(propsFinder.getUserProperties()).thenReturn(tmpUsersPropsFile);
            ThermostatUserSetup userSetup = new ThermostatUserSetup(propsFinder, mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class)) {
                
                @Override
                Properties readPropsFromFile(File propsFile) {
                    return new Properties();
                }
            };
            userSetup.writeUsers(userCreds);
            userSetup = new ThermostatUserSetup(propsFinder, mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class));
            // write same users again
            userSetup.writeUsers(userCreds);
            List<String> expectedLines = new ArrayList<>();
            expectedLines.add("testuser1=t");
            expectedLines.add("speedy-user=bar");
            verifyFileContainsLines(tmpUsersPropsFile, expectedLines);
            assertEquals("expected testuser1=t only once in file.", 1, getNumOccurance(expectedLines.get(0), getLinesAsList(tmpUsersPropsFile)));
            assertEquals("expected speedy-user=bar only once in file.", 1, getNumOccurance(expectedLines.get(1), getLinesAsList(tmpUsersPropsFile)));
        } finally {
            Files.delete(tmpUsersPropsFile.toPath());
        }
    }
    
    @Test
    public void writingRolesDoesnotProduceDuplicates() throws IOException {
        Map<String, CommentedRolePropertyValue> roleProps = new HashMap<>();
        String roleValue = "thermostat-login, \nthermostat-test";
        CommentedRolePropertyValue thermostatAgent = new CommentedRolePropertyValue(roleValue, "some comment");
        roleProps.put("thermostat-agent", thermostatAgent);
        CommentedRolePropertyValue agentUser = new CommentedRolePropertyValue("thermostat-agent", "agent user");
        roleProps.put("foo-agent-user", agentUser);
        
        UserPropertiesFinder propsFinder = mock(UserPropertiesFinder.class);
        File tmpRolesPropsFile = File.createTempFile("thermostat", getClass().getName());
        try {
            when(propsFinder.getRolesProperties()).thenReturn(tmpRolesPropsFile);
            ThermostatUserSetup userSetup = new ThermostatUserSetup(propsFinder, mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class)) {
                
                @Override
                Properties readPropsFromFile(File propsFile) {
                    // simulate empty existing properties
                    return new Properties();
                }
            };
            userSetup.writeRoles(roleProps);
            userSetup = new ThermostatUserSetup(propsFinder, mock(UserCredsValidator.class), mock(CredentialsFileCreator.class), mock(StampFiles.class));
            // write users again
            userSetup.writeRoles(roleProps);
            List<String> expectedList = new ArrayList<>();
            expectedList.add("thermostat-agent=thermostat-login, \\");
            expectedList.add("foo-agent-user=thermostat-agent");
            verifyFileContainsLines(tmpRolesPropsFile, expectedList);
            assertEquals("expected 'thermostat-agent=thermostat-login, \\' only once in file.", 1, getNumOccurance(expectedList.get(0), getLinesAsList(tmpRolesPropsFile)));
            assertEquals("expected 'foo-agent-user=thermostat-agent' only once in file.", 1, getNumOccurance(expectedList.get(1), getLinesAsList(tmpRolesPropsFile)));
        } finally {
            Files.delete(tmpRolesPropsFile.toPath());
        }
    }

    private int getNumOccurance(String itemToSearch, List<String> list) {
        int count = 0;
        for (String line: list) {
            if (line.equals(itemToSearch)) {
                count++;
            }
        }
        return count;
    }

    private void verifyFileContainsLines(File file, List<String> expectedList) throws FileNotFoundException, IOException {
        Set<String> fileContents = getLinesAsSet(file);
        for (String line: expectedList) {
            assertTrue("Expected " + fileContents + " to contain line ' " + line + "'.", fileContents.contains(line));
        }
    }

    private Set<String> getLinesAsSet(File file) throws IOException,
            FileNotFoundException {
        Set<String> fileContents = new HashSet<>();
        try (FileInputStream fin = new FileInputStream(file);
             Scanner fileScanner = new Scanner(fin)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                fileContents.add(line);
            }
        }
        return fileContents;
    }
    
    private List<String> getLinesAsList(File file) throws IOException {
        return Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
    }
}
