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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.Test;

public class TokenManagerTest {

    /**
     * Since we want to make sure that the action-name parameter for
     * CMD channel interactions are trustworthy, we SHA256 hash the client
     * token + the action name parameter, convert the resulting bytes into
     * a hex string and finally put it into the tokens map. This way
     * verification of the token should never pass if either the client token
     * or the action-name was wrong.
     * 
     * This test verifies that we have SHA256 hex-strings as keys. SHA256 hex
     * strings are generated using the the client token first and then the
     * action name for the digest.
     */
    @Test
    public void generateTokenTest() {
        TokenManager tokenManager = new TokenManager();
        String clientToken = "something";
        String action = "myAction";
        byte[] token = tokenManager.generateToken(clientToken.getBytes(), action);
        String expectedKey = "8d51bc31b39f54a1a12f2f94f09371ad5afe2263c1fdb7a3785aaacea6a386ef";
        byte[] actualToken = tokenManager.getStoredToken(expectedKey);
        assertNotNull(actualToken);
        assertTrue(Arrays.equals(token, actualToken));
    }
    
    @Test
    public void canConvertBytesToHexString() throws UnsupportedEncodingException {
        byte[] expected = new byte[] {
                (byte)0xff, 0x6f, 0x6d, 0x65, 0x53, 0x74, 0x72, 0x69, 0x6e, 0x67, 0x57, 0x65, 0x48, 0x61, 0x73, 0x68 
        };
        TokenManager tokenManager = new TokenManager();
        String actual = tokenManager.convertBytesToHexString(expected);
        assertEquals(expected.length * 2, actual.length());
        assertEquals("ff6f6d65537472696e67576548617368", actual);
    }
    
    @Test
    public void generateAndVerifyTokenTest() {
        TokenManager tokenManager = new TokenManager();
        String clientToken = "something";
        String action = "myAction";
        byte[] token = tokenManager.generateToken(clientToken.getBytes(), action);
        assertTrue(tokenManager.verifyToken(clientToken.getBytes(), token, action));
        // try again with different action name, which should not verify
        String wrongAction = "someAction";
        assertFalse(tokenManager.verifyToken(clientToken.getBytes(), token, wrongAction));
    }
}
