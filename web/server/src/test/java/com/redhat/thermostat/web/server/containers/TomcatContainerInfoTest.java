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

package com.redhat.thermostat.web.server.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TomcatContainerInfoTest {

    static final String TOMCAT7_SERVER_INFO = "Apache Tomcat/7.0.35";
    static final String TOMCAT6_SERVER_INFO = "Apache Tomcat/6.0.37";
    
    @Test
    public void testTomcat7ContainerInfo() {
        ServletContainerInfo info = new TomcatContainerInfo(TOMCAT7_SERVER_INFO);
        assertNotNull(info);
        assertEquals(ContainerName.TOMCAT, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNotNull(version);
        assertEquals(7, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(35, version.getMicro());
        assertEquals(ContainerVersion.UNKNOWN_SUFFIX, version.getSuffix());
        assertEquals("7.0.35." + ContainerVersion.UNKNOWN_SUFFIX, version.toString());
    }
    
    @Test
    public void testTomcat6ContainerInfo() {
        ServletContainerInfo info = new TomcatContainerInfo(TOMCAT6_SERVER_INFO);
        assertNotNull(info);
        assertEquals(ContainerName.TOMCAT, info.getName());
        ContainerVersion version = info.getContainerVersion();
        assertNotNull(version);
        assertEquals(6, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(37, version.getMicro());
        assertEquals(ContainerVersion.UNKNOWN_SUFFIX, version.getSuffix());
        assertEquals("6.0.37." + ContainerVersion.UNKNOWN_SUFFIX, version.toString());
    }
    
    @Test
    public void testNull() {
        try {
            new TomcatContainerInfo(null);
            fail("should have failed to instantiate");
        } catch (NullPointerException e) {
            // pass
        }
    }
    
    @Test
    public void testEmpty() {
        try {
            new TomcatContainerInfo("");
            fail("should have failed to instantiate");
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal server info: ''", e.getMessage());
        }
    }
}
