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

package com.redhat.thermostat.vm.gc.common.params;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaVersionTest {

    public static final JavaVersion.VersionPoints ONE_ZERO_ZERO_U0 = new JavaVersion.VersionPoints(1, 0, 0, 0);
    public static final JavaVersion.VersionPoints ONE_ZERO_ONE_U0 = new JavaVersion.VersionPoints(1, 0, 1, 0);
    public static final JavaVersion.VersionPoints ONE_ONE_ZERO_U10 = new JavaVersion.VersionPoints(1, 1, 0, 10);
    public static final JavaVersion.VersionPoints TWO_ZERO_TEN_U40 = new JavaVersion.VersionPoints(2, 0, 10, 40);
    static final JavaVersion VERSION_ONE_ZERO_ZERO_U0 = new JavaVersion(ONE_ZERO_ZERO_U0);
    static final JavaVersion VERSION_ONE_ZERO_ONE_U0 = new JavaVersion(ONE_ZERO_ONE_U0);
    static final JavaVersion VERSION_ONE_ONE_ZERO_U10 = new JavaVersion(ONE_ONE_ZERO_U10);
    static final JavaVersion VERSION_TWO_ZERO_TEN_U40 = new JavaVersion(TWO_ZERO_TEN_U40);

    static final JavaVersion SMALL_RANGE = new JavaVersion(ONE_ZERO_ZERO_U0, ONE_ZERO_ONE_U0);
    static final JavaVersion LARGE_RANGE = new JavaVersion(ONE_ZERO_ZERO_U0, TWO_ZERO_TEN_U40);

    @Test
    public void testValidSingleVersionComparisons() {
        assertFalse(ONE_ZERO_ZERO_U0 + " = " + ONE_ZERO_ZERO_U0, lessThan(VERSION_ONE_ZERO_ZERO_U0, VERSION_ONE_ZERO_ZERO_U0));
        assertTrue(ONE_ZERO_ZERO_U0 + " < " + ONE_ZERO_ONE_U0, lessThan(VERSION_ONE_ZERO_ZERO_U0, VERSION_ONE_ZERO_ONE_U0));
        assertTrue(ONE_ZERO_ONE_U0 + " < " + ONE_ONE_ZERO_U10, lessThan(VERSION_ONE_ZERO_ONE_U0, VERSION_ONE_ONE_ZERO_U10));
        assertTrue(ONE_ONE_ZERO_U10 + " < " + TWO_ZERO_TEN_U40, lessThan(VERSION_ONE_ONE_ZERO_U10, VERSION_TWO_ZERO_TEN_U40));
    }

    @Test
    public void testContains() {
        assertTrue(VERSION_ONE_ZERO_ZERO_U0 + " in " + SMALL_RANGE, SMALL_RANGE.contains(VERSION_ONE_ZERO_ZERO_U0));
        assertTrue(VERSION_ONE_ONE_ZERO_U10 + " in " + LARGE_RANGE, LARGE_RANGE.contains(VERSION_ONE_ONE_ZERO_U10));
        assertFalse(VERSION_TWO_ZERO_TEN_U40 + " in " + SMALL_RANGE, SMALL_RANGE.contains(VERSION_TWO_ZERO_TEN_U40));
        assertTrue(VERSION_TWO_ZERO_TEN_U40 + " in " + LARGE_RANGE, LARGE_RANGE.contains(VERSION_TWO_ZERO_TEN_U40));
        assertTrue(SMALL_RANGE + " in " + LARGE_RANGE, LARGE_RANGE.contains(SMALL_RANGE));
        assertFalse(LARGE_RANGE + " in " + SMALL_RANGE, SMALL_RANGE.contains(LARGE_RANGE));
    }

    @Test
    public void testIsRange() {
        assertTrue(SMALL_RANGE.toString(), SMALL_RANGE.isRange());
        assertTrue(LARGE_RANGE.toString(), LARGE_RANGE.isRange());
        assertFalse(VERSION_ONE_ZERO_ZERO_U0.toString(), VERSION_ONE_ZERO_ZERO_U0.isRange());
        assertFalse(VERSION_TWO_ZERO_TEN_U40.toString(), VERSION_TWO_ZERO_TEN_U40.isRange());
    }

    @Test
    public void testGetLowerBound() {
        assertEquals(new JavaVersion(ONE_ZERO_ZERO_U0), VERSION_ONE_ZERO_ZERO_U0.getLowerBound());
        assertEquals(VERSION_ONE_ZERO_ZERO_U0, VERSION_ONE_ZERO_ZERO_U0.getLowerBound());
        assertEquals(VERSION_ONE_ZERO_ZERO_U0, SMALL_RANGE.getLowerBound());
    }

    @Test
    public void testGetUpperBound() {
        assertEquals(new JavaVersion(ONE_ZERO_ZERO_U0), VERSION_ONE_ZERO_ZERO_U0.getUpperBound());
        assertEquals(VERSION_ONE_ZERO_ZERO_U0, VERSION_ONE_ZERO_ZERO_U0.getUpperBound());
        assertEquals(VERSION_TWO_ZERO_TEN_U40, LARGE_RANGE.getUpperBound());
    }

    @Test(expected = NumberFormatException.class)
    public void testExtremelyLargeVersionNumberCannotBeParsed() {
        JavaVersion version = JavaVersion.fromString(new Long((long)Integer.MAX_VALUE + 1).toString() + ".2.3_4");
        String expectedVersion = "2147483648.2.3_4";
        String expectedRange = expectedVersion + JavaVersion.RANGE_DELIMITER + expectedVersion;
        assertEquals(expectedRange, version.toString());
        assertTrue(version.toString() + " = " + version.toString(), version.compareTo(version) == 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNotAccepted() {
        new JavaVersion(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMajorDisallowed() {
        new JavaVersion.VersionPoints(-1, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMinorDisallowed() {
        new JavaVersion.VersionPoints(0, -1, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMicroDisallowed() {
        new JavaVersion.VersionPoints(0, 0, -1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeUpdateDisallowed() {
        new JavaVersion.VersionPoints(0, 0, 0, -1);
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testInvalidTextNotAccepted() {
        JavaVersion.fromString("INVALID TEXT");
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAccepted() {
        JavaVersion.fromString("1:0:2");
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAccepted2() {
        JavaVersion.fromString("1#3.3_4");
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAccepted3() {
        JavaVersion.fromString("1,300,6_3");
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAcceptedWithNull() {
        JavaVersion.fromString(null);
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testFromStringDoesNotAcceptNegative() {
        JavaVersion.fromString("-1.0.1");
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testRangeMustAscend() {
        JavaVersion.fromString("2.0.0_10:1.0.0_0");
    }

    @Test(expected = JavaVersion.InvalidJavaVersionFormatException.class)
    public void testRangeMustAscend2() {
        new JavaVersion(new JavaVersion.VersionPoints(2, 0, 0, 10), new JavaVersion.VersionPoints(1, 0, 0, 0));
    }

    private static boolean lessThan(JavaVersion a, JavaVersion b) {
        return a.compareTo(b) < 0;
    }

}
