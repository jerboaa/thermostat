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

package com.redhat.thermostat.dev.populator;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArgumentsTest {

    @Test
    public void canParseCorrectArgs() {
        String[] args = new String[] {
                "--username", "foo-user", "--dbUrl", "mongodb://127.0.0.1:27518",
                "--password", "foo-password",
                "--config", "path/to/config"
        };
        Arguments parsedArgs = Arguments.processArguments(args);
        assertValues(parsedArgs);
        // different order
        args = new String[] {
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--username", "foo-user", 
                "--password", "foo-password",
                "--config", "path/to/config"
        };
        parsedArgs = Arguments.processArguments(args);
        assertValues(parsedArgs);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void failToParseUsernameMissing() {
        String[] args = new String[] {
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--password", "foo-password",
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--config", "path/to/config"
        };
        Arguments.processArguments(args);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void failToParsePasswordMissing() {
        String[] args = new String[] {
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--username", "foo-user",
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--config", "path/to/config"
        };
        Arguments.processArguments(args);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void failToParseDbUrlMissing() {
        String[] args = new String[] {
                "--username", "foo-user",
                "--username", "foo-user",
                "--password", "foo-password",
                "--config", "path/to/config"
        };
        Arguments.processArguments(args);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void failToParseIllegalOptionValue() {
        String[] args = new String[] {
                "--username", "--foo-user", // -- in foo-user is wrong
                "--username", "foo-user",
                "--password", "foo-password",
                "--config", "path/to/config"
        };
        Arguments.processArguments(args);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void failToParseIllegalOption() {
        String[] args = new String[] {
                "--username", "--foo-user",
                "username", "foo-user", // epxected --username not username
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--password", "foo-password",
                "--config", "path/to/config"
        };
        Arguments.processArguments(args);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void failToParseIllegalLength() {
        String[] args = new String[] {
                "--username", "--foo-user",
                "--username", "foo-user",
                "--dbUrl", "mongodb://127.0.0.1:27518",
                "--password", "foo-password",
                "--config", "path/to/config"
        };
        Arguments.processArguments(args);
    }
    
    private void assertValues(Arguments parsedArgs) {
        assertEquals("mongodb://127.0.0.1:27518", parsedArgs.getDbUrl());
        assertEquals("foo-user", parsedArgs.getUsername());
        assertEquals("foo-password", parsedArgs.getPassword());
        assertEquals("path/to/config", parsedArgs.getConfigFile());
    }
}
