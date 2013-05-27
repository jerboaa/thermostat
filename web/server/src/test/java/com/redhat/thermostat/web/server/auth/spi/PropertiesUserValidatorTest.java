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

package com.redhat.thermostat.web.server.auth.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class PropertiesUserValidatorTest {
    
    private PropertiesUserValidator validator;

    @Before
    public void setup() {
        URL testFile = this.getClass().getResource("/test_users.properties");
        validator = new PropertiesUserValidator(testFile.getFile());   
    }
    
    @After
    public void tearDown() {
        validator = null;
    }
    
    
    @Test
    public void authenticationFailsForNonExistingUser() {
        try {
            validator.authenticate("does not exist", null);
            fail("user does not exist in file");
        } catch (UserValidationException e) {
            // pass
            assertEquals("User 'does not exist' not found", e.getMessage());
        }
    }
    
    @Test
    public void passwordMatch() {
        try {
            validator.authenticate("user1", "passwd1".toCharArray());
            validator.authenticate("strange\nuser", "testMe".toCharArray());
            validator.authenticate("c102892{0}", "test".toCharArray());
            // extra spaces are remved from properties system
            validator.authenticate("multiuser", "blah\ntest test".toCharArray());
            // '\0' in test_users.properties become '0'
            validator.authenticate("strange0user", "test0Me".toCharArray());
            // pass
        } catch (UserValidationException e) {
            e.printStackTrace();
            fail("should have been able to authenticate user");
        }
    }
    
    @Test
    public void passwordMismatch() {
        try {
            validator.authenticate("user1", "passwD1".toCharArray());
            fail("password does not match!");
        } catch (UserValidationException e) {
            assertEquals("Login failed!", e.getMessage());
        }
        try {
            validator.authenticate("c102892{0}", "passwd1".toCharArray());
        } catch (UserValidationException e) {
            assertEquals("Login failed!", e.getMessage());
        }
    }
    
    @Test
    public void testInit() {
        try {
            new PropertiesUserValidator();
            fail("THERMOSTAT_HOME not set, should have failed!");
        } catch (InvalidConfigurationException e) {
            // pass
            assertTrue(e.getMessage().contains("THERMOSTAT_HOME"));
        }
    }
    
    @Test
    public void testInitWithMissingFile() {
        try {
            new PropertiesUserValidator("file/which/does/not/exist");
        } catch (InvalidConfigurationException e) {
            // pass
            assertEquals("Unable to load user database", e.getMessage());
        }
    }
    
    @Test
    public void canGetUserSet() {
        Set<String> expectedSet = new HashSet<>(10);
        expectedSet.add("user1");
        expectedSet.add("testing");
        expectedSet.add("multiuser");
        expectedSet.add("c102892{0}");
        expectedSet.add("strange\nuser");
        expectedSet.add("strange0user");
        Set<Object> actual = validator.getAllKnownUsers();
        assertTrue(expectedSet.equals(actual));
        expectedSet.remove("user1");
        assertFalse(expectedSet.equals(actual));
    }
}
