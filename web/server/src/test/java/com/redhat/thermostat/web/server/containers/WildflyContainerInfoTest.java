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

package com.redhat.thermostat.web.server.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class WildflyContainerInfoTest {

    // JBoss AS uses JBoss Web as servlet container
    static final String JBOSS_AS7_SERVER_INFO = "JBoss Web/7.0.13.Final";
    // wildfly uses undertow as it's servlet container: see http://undertow.io/
    static final String WILDFLY8_SERVER_INFO = "Undertow - 1.0.0.Beta17";
    
    @Test
    public void testJBossAS7ContainerInfo() {
        ServletContainerInfo info = new WildflyContainerInfo(JBOSS_AS7_SERVER_INFO);
        assertNotNull(info);
        assertEquals(ContainerName.WILDFLY, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNotNull(version);
        assertEquals(7, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(13, version.getMicro());
        assertEquals("Final", version.getSuffix());
        assertEquals("7.0.13.Final", version.toString());
    }
    
    @Test
    public void testWildfly8ContainerInfo() {
        ServletContainerInfo info = new WildflyContainerInfo(WILDFLY8_SERVER_INFO);
        assertNotNull(info);
        assertEquals(ContainerName.WILDFLY, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNotNull(version);
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getMicro());
        assertEquals("Beta17", version.getSuffix());
        assertEquals("1.0.0.Beta17", version.toString());
    }
    
    @Test
    public void testUnknownWildflyServerInfo() {
        ServletContainerInfo info = new WildflyContainerInfo("foo - 1.1.1.beta");
        assertNotNull(info);
        assertEquals(ContainerName.WILDFLY, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNull(version);
    }
    
    @Test
    public void testNull() {
        try {
            new WildflyContainerInfo(null);
            fail("should have failed to instantiate");
        } catch (NullPointerException e) {
            // pass
        }
    }
    
    @Test
    public void testEmpty() {
        try {
            new WildflyContainerInfo("");
            fail("should have failed to instantiate");
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal server info: ''", e.getMessage());
        }
    }
}
