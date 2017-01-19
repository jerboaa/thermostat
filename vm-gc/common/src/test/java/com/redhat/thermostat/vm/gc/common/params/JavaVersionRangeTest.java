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

package com.redhat.thermostat.vm.gc.common.params;

import org.junit.Assert;
import org.junit.Test;

import com.redhat.thermostat.vm.gc.common.params.JavaVersionRange.VersionPoints;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaVersionRangeTest {

    public static final JavaVersionRange.VersionPoints ONE_ZERO_ZERO_U0 = new JavaVersionRange.VersionPoints(1, 0, 0, 0);
    public static final JavaVersionRange.VersionPoints ONE_ZERO_ONE_U0 = new JavaVersionRange.VersionPoints(1, 0, 1, 0);
    public static final JavaVersionRange.VersionPoints ONE_ONE_ZERO_U10 = new JavaVersionRange.VersionPoints(1, 1, 0, 10);
    public static final JavaVersionRange.VersionPoints TWO_ZERO_TEN_U40 = new JavaVersionRange.VersionPoints(2, 0, 10, 40);
    static final JavaVersionRange VERSION_ONE_ZERO_ZERO_U0 = new JavaVersionRange(ONE_ZERO_ZERO_U0);
    static final JavaVersionRange VERSION_ONE_ZERO_ONE_U0 = new JavaVersionRange(ONE_ZERO_ONE_U0);
    static final JavaVersionRange VERSION_ONE_ONE_ZERO_U10 = new JavaVersionRange(ONE_ONE_ZERO_U10);
    static final JavaVersionRange VERSION_TWO_ZERO_TEN_U40 = new JavaVersionRange(TWO_ZERO_TEN_U40);

    static final JavaVersionRange SMALL_RANGE = new JavaVersionRange(ONE_ZERO_ZERO_U0, true, ONE_ZERO_ONE_U0, true);
    static final JavaVersionRange LARGE_RANGE = new JavaVersionRange(ONE_ZERO_ZERO_U0, true, TWO_ZERO_TEN_U40, true);

    @Test
    public void testVendorStringInVersion() {
        final String winRawStr = "1.8.0_101-1-comment";
        final String oldRawStr = "1.8.0_101-1";
        final JavaVersionRange winRange = JavaVersionRange.fromString(winRawStr);
        final JavaVersionRange oldRange = JavaVersionRange.fromString(oldRawStr);
        final String winRangeStr = winRange.toString();
        final String oldRangeStr = oldRange.toString();
        Assert.assertEquals("must strip off '-comment'", oldRangeStr, winRangeStr);
    }

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
        assertEquals(new JavaVersionRange(ONE_ZERO_ZERO_U0), VERSION_ONE_ZERO_ZERO_U0.getLowerBound());
        assertEquals(VERSION_ONE_ZERO_ZERO_U0, VERSION_ONE_ZERO_ZERO_U0.getLowerBound());
        assertEquals(VERSION_ONE_ZERO_ZERO_U0, SMALL_RANGE.getLowerBound());
    }

    @Test
    public void testGetUpperBound() {
        assertEquals(new JavaVersionRange(ONE_ZERO_ZERO_U0), VERSION_ONE_ZERO_ZERO_U0.getUpperBound());
        assertEquals(VERSION_ONE_ZERO_ZERO_U0, VERSION_ONE_ZERO_ZERO_U0.getUpperBound());
        assertEquals(VERSION_TWO_ZERO_TEN_U40, LARGE_RANGE.getUpperBound());
    }

    @Test
    public void testSingleVersionIsShortcutForSameWithComma() {
        JavaVersionRange single = JavaVersionRange.fromString("[1.0.0.0]");
        JavaVersionRange pair = JavaVersionRange.fromString("[1.0.0.0,1.0.0.0]");
        assertEquals(single, pair);
    }

    @Test
    public void testDefaultIsInclusive() {
        JavaVersionRange version = new JavaVersionRange(1, 0, 0, 0);
        assertTrue(version.isLowerBoundInclusive());
        assertTrue(version.isUpperBoundInclusive());
    }

    @Test
    public void testLowerBoundInclusive() {
        JavaVersionRange inclusive = new JavaVersionRange(new JavaVersionRange.VersionPoints(2, 0, 0, 0), true,
                new JavaVersionRange.VersionPoints(2, 1, 0, 0), false); // [2.0.0.0, 2.1.0.0)
        JavaVersionRange exclusive = new JavaVersionRange(new JavaVersionRange.VersionPoints(2, 0, 0, 0), false,
                new JavaVersionRange.VersionPoints(2, 1, 0, 0), false); // (2.0.0.0, 2.1.0.0)
        JavaVersionRange version = new JavaVersionRange(2, 0, 0, 0);
        assertTrue(inclusive.contains(version));
        assertFalse(exclusive.contains(version));
    }

    @Test
    public void testUpperBoundInclusive() {
        JavaVersionRange inclusive = new JavaVersionRange(new JavaVersionRange.VersionPoints(2, 0, 0, 0), false,
                new JavaVersionRange.VersionPoints(2, 1, 0, 0), true); // (2.0.0.0, 2.1.0.0]
        JavaVersionRange exclusive = new JavaVersionRange(new JavaVersionRange.VersionPoints(2, 0, 0, 0), false,
                new JavaVersionRange.VersionPoints(2, 1, 0, 0), false); // (2.0.0.0, 2.1.0.0)
        JavaVersionRange version = new JavaVersionRange(2, 1, 0, 0);
        assertTrue(inclusive.contains(version));
        assertFalse(exclusive.contains(version));
    }

    @Test(expected = NumberFormatException.class)
    public void testExtremelyLargeVersionNumberCannotBeParsed() {
        JavaVersionRange version = JavaVersionRange.fromString("[" + Long.toString((long) Integer.MAX_VALUE + 1) + ".2.3_4" + "]");
        String expectedVersion = "[2147483648.2.3_4]";
        String expectedRange = "[" + expectedVersion + "," + expectedVersion + "]";
        assertEquals(expectedRange, version.toString());
        assertTrue(version.toString() + " = " + version.toString(), version.compareTo(version) == 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNotAccepted() {
        new JavaVersionRange(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMajorDisallowed() {
        new JavaVersionRange.VersionPoints(-1, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMinorDisallowed() {
        new JavaVersionRange.VersionPoints(0, -1, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMicroDisallowed() {
        new JavaVersionRange.VersionPoints(0, 0, -1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeUpdateDisallowed() {
        new JavaVersionRange.VersionPoints(0, 0, 0, -1);
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testInvalidTextNotAccepted() {
        JavaVersionRange.fromString("INVALID TEXT");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAccepted() {
        JavaVersionRange.fromString("[1,0,2]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAccepted2() {
        JavaVersionRange.fromString("[1#3.3_4]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAccepted3() {
        JavaVersionRange.fromString("[1,300,6_3]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testInvalidFormatNotAcceptedWithNull() {
        JavaVersionRange.fromString(null);
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testFromStringDoesNotAcceptNegative() {
        JavaVersionRange.fromString("[-1.0.1]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testRangeMustAscend() {
        JavaVersionRange.fromString("[2.0.0_10,1.0.0_0]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testRangeMustAscend2() {
        new JavaVersionRange(new JavaVersionRange.VersionPoints(2, 0, 0, 10), true, new JavaVersionRange.VersionPoints(1, 0, 0, 0), true);
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testRangeMustNotBeEmpty() {
        JavaVersionRange.fromString("(1.0.0.0)");
    }

    @Test
    public void testBracketsAreOptionalForSingleVersion() {
        JavaVersionRange without = JavaVersionRange.fromString("1.0.0.0");
        JavaVersionRange with = JavaVersionRange.fromString("[1.0.0.0]");
        assertEquals(without, with);
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testMismatchedBracketsForSingleVersion() {
        JavaVersionRange.fromString("[1.0.0.0");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testMismatchedBracketsForSingleVersion2() {
        JavaVersionRange.fromString("1.0.0.0]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testBracketsAreNotOptionalForRanges() {
        JavaVersionRange.fromString("1.0.0.0,2.0.0.0");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testBracketsAreNotOptionalForRanges2() {
        JavaVersionRange.fromString("[1.0.0.0,2.0.0.0");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testBracketsAreNotOptionalForRanges3() {
        JavaVersionRange.fromString("1.0.0.0,2.0.0.0]");
    }

    @Test
    public void testUnboundedUpperBound() {
        int max = Integer.MAX_VALUE;
        JavaVersionRange unbounded = JavaVersionRange.fromString("[1.0.0.0,]");
        assertTrue(unbounded.isLowerBoundInclusive());
        assertTrue(unbounded.isUpperBoundInclusive());
        assertTrue(unbounded.getLowerBound().equals(new JavaVersionRange(1, 0, 0, 0)));
        assertTrue(unbounded.getUpperBound().equals(new JavaVersionRange(max, max, max, max)));
    }

    @Test
    public void testUnboundedLowerBound() {
        JavaVersionRange unbounded = JavaVersionRange.fromString("[,1.0.0.0]");
        assertTrue(unbounded.isLowerBoundInclusive());
        assertTrue(unbounded.isUpperBoundInclusive());
        assertTrue(unbounded.getLowerBound().equals(new JavaVersionRange(0, 0, 0, 0)));
        assertTrue(unbounded.getUpperBound().equals(new JavaVersionRange(1, 0, 0, 0)));
    }

    @Test
    public void testUnboundedUpperBoundExclusiveLowerBound() {
        JavaVersionRange version = JavaVersionRange.fromString("(1.2.0.0,)");
        JavaVersionRange one = JavaVersionRange.fromString("1.0.0.0");
        JavaVersionRange two = JavaVersionRange.fromString("1.2.0.0");
        JavaVersionRange three = JavaVersionRange.fromString("1.3.0.0");
        assertFalse(version.contains(one));
        assertFalse(version.contains(two));
        assertTrue(version.contains(three));
    }

    @Test
    public void testUnboundedLowerBoundExclusiveUpperBound() {
        JavaVersionRange version = JavaVersionRange.fromString("[,1.8.0.0)");
        JavaVersionRange two = JavaVersionRange.fromString("1.2.0.0");
        JavaVersionRange seven = JavaVersionRange.fromString("1.7.0.0");
        JavaVersionRange lateSeven = JavaVersionRange.fromString("1.7.0.81");
        JavaVersionRange eight = JavaVersionRange.fromString("1.8.0.0");
        JavaVersionRange lateEight = JavaVersionRange.fromString("1.8.0_51");
        assertTrue(version.contains(two));
        assertTrue(version.contains(seven));
        assertTrue(version.contains(lateSeven));
        assertFalse(version.contains(eight));
        assertFalse(version.contains(lateEight));
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testVersionsRequiredOnAtLeastOneSideOfCommaWithBrackets() {
        JavaVersionRange.fromString("[,]");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testVersionsRequiredOnBothSidesOfCommaWithoutBrackets() {
        JavaVersionRange.fromString("1.0.0.0,");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testVersionsRequiredOnBothSidesOfCommaWithoutBrackets2() {
        JavaVersionRange.fromString(",1.0.0.0");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testVersionsRequiredOnBothSidesOfCommaWithoutBrackets3() {
        JavaVersionRange.fromString(",");
    }

    @Test
    public void testUnderscoreEquivalentToDot() {
        JavaVersionRange score = JavaVersionRange.fromString("1.2.3_4");
        JavaVersionRange dot = JavaVersionRange.fromString("1.2.3.4");
        assertEquals(score, dot);
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testScoreOnlyAllowedAsFinalSeparator() {
        JavaVersionRange.fromString("1_2.3.4");
    }

    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void testScoreOnlyAllowedAsFinalSeparator2() {
        JavaVersionRange.fromString("1.2_3.4");
    }
    
    @Test
    public void canParseCustomBuiltJDKVersion() {
        testJDKVersion("1.7.0-internal"); // a custom JDK 7 build has this version string
        testJDKVersion("1.7.0");
        testJDKVersion("1.8.0");
        testJDKVersion("1.8.0.55-internal");
    }
    
    @Test
    public void testVersionPointsFromString() {
        VersionPoints points = VersionPoints.fromString("1.7.0-internal");
        assertEquals(1, points.getMajor());
        assertEquals(7, points.getMinor());
        assertEquals(0, points.getMicro());
        assertEquals(0, points.getUpdate());
        assertEquals("internal", points.getPreRelease());
        assertEquals("1.7.0.0-internal", points.toString());
        
        points = VersionPoints.fromString("1.7.0");
        assertEquals(1, points.getMajor());
        assertEquals(7, points.getMinor());
        assertEquals(0, points.getMicro());
        assertEquals(0, points.getUpdate());
        assertEquals("", points.getPreRelease());
        assertEquals("1.7.0.0", points.toString());
        
        points = VersionPoints.fromString("1.8.1_55-b20");
        assertEquals(1, points.getMajor());
        assertEquals(8, points.getMinor());
        assertEquals(1, points.getMicro());
        assertEquals(55, points.getUpdate());
        assertEquals("b20", points.getPreRelease());
        assertEquals("1.8.1.55-b20", points.toString());
    }
    
    @Test(expected = JavaVersionRange.InvalidJavaVersionFormatException.class)
    public void shouldFailToParseVersionPointsForIrregularVersion() {
        VersionPoints.fromString("1.7.0|30-internal"); // illegal update separator
    }
    
    private void testJDKVersion(String jdkVersion) {
        JavaVersionRange version = null;
        try {
            version = JavaVersionRange.fromString(jdkVersion);
            // pass
        } catch (JavaVersionRange.InvalidJavaVersionFormatException e) {
            fail("Expected to be able to parse " + jdkVersion);
        }
        assertFalse(version.isRange());
    }

    private static boolean lessThan(JavaVersionRange a, JavaVersionRange b) {
        return a.compareTo(b) < 0;
    }

}
