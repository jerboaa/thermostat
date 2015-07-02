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

package com.redhat.thermostat.launcher.internal;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class ShellArgsParserTest {

    @Test
    public void testEmptyString() {
        assertEmpty("");
    }

    @Test
    public void testSingleArg() {
        assertResult("foo", "foo");
    }

    @Test
    public void testTwoArgs() {
        assertResult("foo bar", "foo", "bar");
    }

    @Test
    public void testThreeArgs() {
        assertResult("foo bar baz", "foo", "bar", "baz");
    }

    @Test
    public void testLeadingSpaces() {
        assertResult("    foo", "foo");
    }

    @Test
    public void testTrailingSpaces() {
        assertResult("foo    ", "foo");
    }

    @Test
    public void testInnerSpaces() {
        assertResult("foo    bar", "foo", "bar");
    }

    @Test
    public void testLotsOfSpaces() {
        assertResult("    foo    bar    ", "foo", "bar");
    }

    @Test
    public void testOnlySpaces() {
        assertEmpty("    ");
    }

    @Test
    public void testTabCharacter() {
        assertResult("foo\tbar", "foo", "bar");
    }

    @Test
    public void testQuotedArg() {
        assertResult("\"foo\"", "foo");
    }

    @Test
    public void testQuotedString() {
        assertResult("\"foo bar\"", "foo bar");
    }

    @Test
    public void testSingleStartingQuote() {
        // malformed argument
        assertResult("\"foo", "foo");
    }

    @Test
    public void testSingleEndingQuote() {
        // malformed argument
        assertResult("foo\"", "foo\"");
    }

    @Test
    public void testSingleMiddleQuote() {
        // malformed argument
        assertResult("foo \" bar", "foo", " bar");
    }

    @Test
    public void testSingleEscapedQuote() {
        assertResult("foo\\\"", "foo\"");
    }

    @Test
    public void testQuoteContainingEscapedQuoteLiteral() {
        assertResult("\"foo \\\" bar\"", "foo \" bar");
    }

    @Test
    public void testQuoteContainingEscapedQuoteLiteral2() {
        assertResult("\"foo \\\"\"", "foo \"");
    }

    @Test
    public void testQuotedEmptyString() {
        assertResult("\"\"", "");
    }

    @Test
    public void testQuotedSpacesString() {
        assertResult("\" \"", " ");
    }

    private void assertEmpty(String input) {
        ShellArgsParser sap = new ShellArgsParser(input);
        String[] result = sap.parse();
        assertArrayEquals(new String[]{}, result);
    }

    private void assertResult(String input, String ... expecteds) {
        ShellArgsParser sap = new ShellArgsParser(input);
        String[] result = sap.parse();
        assertArrayEquals(expecteds, result);
    }
}
