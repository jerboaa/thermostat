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

package com.redhat.thermostat.common.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.net.ssl.X509KeyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JSSEKeyManagerTest {

    private X509KeyManager tm;
    
    @Before
    public void setup() {
        this.tm = mock(X509KeyManager.class);
    }
    
    @After
    public void teardown() {
        this.tm = null;
    }
    
    @Test
    public void chooseServerAliasReturnsThermostat() {
        JSSEKeyManager keyManager = new JSSEKeyManager(tm);
        assertEquals(JSSEKeyManager.THERMOSTAT_KEY_ALIAS,
                keyManager.chooseServerAlias(null, null, null));
    }
    
    @Test
    public void testKeyAliasIsThermostat() {
        // In documentation we tell our users that the keyalias for the
        // agent server key has to be thermostat.
        // See: http://icedtea.classpath.org/wiki/Thermostat/SecurityConsiderations
        assertEquals(JSSEKeyManager.THERMOSTAT_KEY_ALIAS, "thermostat");
    }
    
    @Test
    public void chooseEngineServerAliasReturnsThermostatAlias() {
        JSSEKeyManager keyManager = new JSSEKeyManager(tm);
        assertEquals(JSSEKeyManager.THERMOSTAT_KEY_ALIAS,
                keyManager.chooseEngineServerAlias(null, null, null));
    }
    
    @Test
    public void otherMethodsDelegate() {
        JSSEKeyManager keyManager = new JSSEKeyManager(tm);
        keyManager.chooseClientAlias(null, null, null);
        verify(tm).chooseClientAlias(null, null, null);
        keyManager.getCertificateChain("blah");
        verify(tm).getCertificateChain("blah");
        keyManager.getClientAliases(null, null);
        verify(tm).getClientAliases(null, null);
        keyManager.getPrivateKey("test");
        verify(tm).getPrivateKey("test");
        keyManager.getServerAliases("something", null);
        verify(tm).getServerAliases("something", null);
    }
}

