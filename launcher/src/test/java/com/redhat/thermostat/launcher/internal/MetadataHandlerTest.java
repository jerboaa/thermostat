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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.launcher.BundleInformation;

import java.util.List;
import java.util.ArrayList;

import junit.framework.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

public class MetadataHandlerTest {

    private static final MetadataHandler handler = new MetadataHandler();

    @Test
    public void testManifestParser() {
        String exportHeader = "com.redhat.thermostat.bundle1;version=\"1.1.1\",com.redhat.thermosta"+
                "t.bundle1.package1;version=\"1.1.1\",com.redhat.thermostat.bundle1.package2;version=\"1.1.2\"";
        List<BundleInformation> exports = handler.parseHeader(exportHeader);
        assertEquals(exports.size(), 3);
        assertTrue(exports.contains(new BundleInformation("com.redhat.thermostat.bundle1", "1.1.1")));
        assertTrue(exports.contains(new BundleInformation("com.redhat.thermostat.bundle1.package1", "1.1.1")));
        assertTrue(exports.contains(new BundleInformation("com.redhat.thermostat.bundle1.package2", "1.1.2")));
    }

    @Test
    public void testExtraDirectives() {
        String importHeader = "com.redhat.thermostat.bundle1;version=\"[9.1,10)" +
                "\",com.redhat.thermostat.bundle1.package1;version=\"[9.1,10)\",com.redhat.thermostat.bundle1." +
                "package2;resolution:=optional;version=\"9.1\"";
        List<BundleInformation> imports = handler.parseHeader(importHeader);
        assertEquals(imports.size(), 3);
        assertTrue(imports.contains(new BundleInformation("com.redhat.thermostat.bundle1", "[9.1,10)")));
        assertTrue(imports.contains(new BundleInformation("com.redhat.thermostat.bundle1.package1", "[9.1,10)")));
        assertTrue(imports.contains(new BundleInformation("com.redhat.thermostat.bundle1.package2", "9.1")));
    }

    @Test
    public void testManifestParser2() {
        String exportHeader = "com.redhat.thermostat.bundle1;version=\"[6.7.1,6.8)\",com.redhat.thermostat" +
                ".bundle1.package1;version=\"[6.8.8,6.8.9]\",com.redhat.thermostat.bundle1.package2;version=\"(9.1.2,9.3)\"";
        List<BundleInformation> exports = handler.parseHeader(exportHeader);
        assertEquals(exports.size(), 3);
        assertTrue(exports.contains(new BundleInformation("com.redhat.thermostat.bundle1", "[6.7.1,6.8)")));
        assertTrue(exports.contains(new BundleInformation("com.redhat.thermostat.bundle1.package1", "[6.8.8,6.8.9]")));
        assertTrue(exports.contains(new BundleInformation("com.redhat.thermostat.bundle1.package2", "(9.1.2,9.3)")));
    }

    @Test
    public void testEmptyHeader() {
        String header = "";
        List<BundleInformation> exports = new ArrayList<>(handler.parseHeader(header));
        assertEquals(0, exports.size());
    }

    @Test
    public void testVersionParser() {
        String versionString = "[1.1.2,1.4.3)";
        String[] versions = handler.parseVersionRange(versionString);
        assertEquals("1.1.2", versions[0]);
        assertEquals("1.4.3", versions[1]);
        int[] lowerBound = handler.extractVersions(versions[0]);
        int[] upperBound = handler.extractVersions(versions[1]);
        assertEquals(1, lowerBound[0]);
        assertEquals(1, lowerBound[1]);
        assertEquals(2, lowerBound[2]);
        assertEquals(1, upperBound[0]);
        assertEquals(4, upperBound[1]);
        assertEquals(3, upperBound[2]);
    }

    @Test
    public void testParserNonFullVersion() {
        String versionString = "[1,3.2]";
        String[] versions = handler.parseVersionRange(versionString);
        Assert.assertEquals("1", versions[0]);
        Assert.assertEquals("3.2", versions[1]);
        // The version extractor returns -1 for missing versions
        int[] lowerBound = handler.extractVersions(versions[0]);
        int[] upperBound = handler.extractVersions(versions[1]);
        assertEquals(1, lowerBound[0]);
        assertEquals(-1, lowerBound[1]);
        assertEquals(-1, lowerBound[2]);
        assertEquals(3, upperBound[0]);
        assertEquals(2, upperBound[1]);
        assertEquals(-1, upperBound[2]);
    }

    @Test
    public void testNotAVersion() {
        String versionString = "foo.bar.baz";
        int[] result = handler.extractVersions(versionString);
        assertEquals(-1, result[0]);
        assertEquals(-1, result[1]);
        assertEquals(-1, result[2]);
    }

    @Test
    public void testVersionMatcher() {
        String versionString = "[1,2]";
        String[] versions = handler.parseVersionRange(versionString);
        int[] lowerBound = handler.extractVersions(versions[0]);
        int[] upperBound = handler.extractVersions(versions[1]);
        assertTrue(handler.satisfiesBound(new int[]{1, 1, 1}, lowerBound, false));
        assertTrue(handler.satisfiesBound(upperBound, new int[]{1, 1, 1}, false));
    }

    @Test
    public void testVersionMatcher2() {
        String versionString = "[1.1,2]";
        String[] versions = handler.parseVersionRange(versionString);
        int[] lowerBound = handler.extractVersions(versions[0]);
        int[] upperBound = handler.extractVersions(versions[1]);
        assertFalse(handler.satisfiesBound(new int[]{1, -1, -1}, lowerBound, false));
        assertTrue(handler.satisfiesBound(upperBound, new int[]{1, -1, -1}, false));
    }

    @Test
    public void testVersionMatcher3() {
        String versionString = "[1.1.1,2]";
        String[] versions = handler.parseVersionRange(versionString);
        int[] lowerBound = handler.extractVersions(versions[0]);
        int[] upperBound = handler.extractVersions(versions[1]);
        assertFalse(handler.satisfiesBound(new int[]{1, 1, -1}, lowerBound, false));
        assertTrue(handler.satisfiesBound(upperBound, new int[]{1, 1, -1}, false));
    }

    @Test
    public void testInclusiveRange() {
        String versionString = "[2.3.4,3)";
        String[] versions = handler.parseVersionRange(versionString);
        int[] upperBound = handler.extractVersions(versions[1]);
        assertTrue(handler.satisfiesBound(upperBound, new int[]{3, -1, -1}, false));
        assertFalse(handler.satisfiesBound(upperBound, new int[]{3, -1, -1}, true));
    }
}
