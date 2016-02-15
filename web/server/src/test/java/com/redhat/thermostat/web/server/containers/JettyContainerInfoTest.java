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

package com.redhat.thermostat.web.server.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class JettyContainerInfoTest {
    
    static final String JETTY8_SERVER_INFO = "jetty/8.1.5.v20120716";
    static final String JETTY9_SERVER_INFO = "jetty/9.1.0.v20131115";

    @Test
    public void testParseJetty8ContainerVersion() {
        JettyContainerInfo info = new JettyContainerInfo(JETTY8_SERVER_INFO);
        assertEquals(ContainerName.JETTY, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNotNull(version);
        assertEquals(8, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(5, version.getMicro());
        assertEquals("v20120716", version.getSuffix());
        assertEquals("8.1.5.v20120716", version.toString());
    }
    
    @Test
    public void testParseJetty9ContainerVersion() {
        JettyContainerInfo info = new JettyContainerInfo(JETTY9_SERVER_INFO);
        assertEquals(ContainerName.JETTY, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNotNull(version);
        assertEquals(9, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(0, version.getMicro());
        assertEquals("v20131115", version.getSuffix());
        assertEquals("9.1.0.v20131115", version.toString());
    }
    
    @Test
    public void testParseEmpty() {
        try {
            new JettyContainerInfo("");
            fail("should have failed to instantiate");
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal server info: ''", e.getMessage());
        }
    }
    
    @Test
    public void testParseNull() {
        try {
            new JettyContainerInfo(null);
            fail("should have failed to instantiate");
        } catch (NullPointerException e) {
            // pass
        }
    }
}
