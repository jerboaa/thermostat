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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class WebServiceLauncherTest {
    
    private WebServiceLauncher launcher;
    
    @After
    public void tearDown() {
        launcher = null;
    }
    
    @Test( expected=InvalidConfigurationException.class )
    public void unsetPortThrowsException() throws Exception {
        launcher = new WebServiceLauncher();
        launcher.setStorageURL("mongodb://127.0.0.1:27518");
        launcher.start();
    }
    
    @Test( expected=InvalidConfigurationException.class )
    public void unsetStorageURLThrowsException() throws Exception {
        launcher = new WebServiceLauncher();
        launcher.setPort(20);
        launcher.start();
    }
    
    @Test
    public void invalidPortThrowsException() throws Exception {
        int excptnsThrown = 0;
        int excptnsExpected = 2;
        launcher = new WebServiceLauncher();
        try {
            launcher.setPort(-10);
            launcher.start();
        } catch (InvalidConfigurationException e) {
            excptnsThrown++;
        }
        try {
            launcher.setPort(0);
            launcher.start();
        } catch (InvalidConfigurationException e) {
            excptnsThrown++;
        }
        assertEquals(excptnsExpected, excptnsThrown);
    }
    
    @Test
    @Ignore("server.start() throws NPE for some reason")
    public void verifyStartDoesStartServer() throws Exception {
        Server server = mock(Server.class);
        WebServiceLauncher launcher = new WebServiceLauncher(server);
        launcher.setPort(50);
        launcher.setStorageURL("mongodb://test.example.org/db");
        launcher.start();
        verify(server).start();
        launcher.stop();
    }

}
