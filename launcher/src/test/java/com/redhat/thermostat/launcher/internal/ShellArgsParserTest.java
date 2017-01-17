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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ShellArgsParserTest {

    private ShellArgsParser.Issues issues;

    @Test(expected = IllegalStateException.class)
    public void testGetIssuesBeforeParse() {
        ShellArgsParser sap = new ShellArgsParser("");
        sap.getParseIssues();
        sap.parse();
    }

    @Test
    public void testEmptyString() {
        assertEmpty("");
        assertNoIssues();
    }

    @Test
    public void testOneCharArg() {
        assertResult("f", "f");
        assertNoIssues();
    }

    @Test
    public void testSingleArg() {
        assertResult("foo", "foo");
        assertNoIssues();
    }

    @Test
    public void testTwoArgs() {
        assertResult("foo bar", "foo", "bar");
        assertNoIssues();
    }

    @Test
    public void testThreeArgs() {
        assertResult("foo bar baz", "foo", "bar", "baz");
        assertNoIssues();
    }

    @Test
    public void testLeadingSpaces() {
        assertResult("    foo", "foo");
        assertNoIssues();
    }

    @Test
    public void testTrailingSpaces() {
        assertResult("foo    ", "foo");
        assertNoIssues();
    }

    @Test
    public void testInnerSpaces() {
        assertResult("foo    bar", "foo", "bar");
        assertNoIssues();
    }

    @Test
    public void testLotsOfSpaces() {
        assertResult("    foo    bar    ", "foo", "bar");
        assertNoIssues();
    }

    @Test
    public void testOnlySpaces() {
        assertEmpty("    ");
        assertNoIssues();
    }

    @Test
    public void testTabCharacter() {
        assertResult("foo\tbar", "foo", "bar");
        assertNoIssues();
    }

    @Test
    public void testQuotedArg() {
        assertResult("\"foo\"", "foo");
        assertNoIssues();
    }

    @Test
    public void testSingleQuotedArg() {
        assertResult("'foo'", "foo");
        assertNoIssues();
    }

    @Test
    public void testQuotedString() {
        assertResult("\"foo bar\"", "foo bar");
        assertNoIssues();
    }

    @Test
    public void testSingleQuotedString() {
        assertResult("'foo bar'", "foo bar");
        assertNoIssues();
    }

    @Test
    public void testQuotedArgFirst() {
        assertResult("\"foo bar\" baz", "foo bar", "baz");
        assertNoIssues();
    }

    @Test
    public void testSingleQuotedArgFirst() {
        assertResult("'foo bar' baz", "foo bar", "baz");
        assertNoIssues();
    }

    @Test
    public void testSingleStartingQuote() {
        assertResult("\"foo", "foo");
        assertIssues(new ShellArgsParser.Issue(0, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testSingleStartingSingleQuote() {
        assertResult("'foo", "foo");
        assertIssues(new ShellArgsParser.Issue(0, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testSingleEndingQuote() {
        assertResult("foo\"", "foo\"");
        assertIssues(new ShellArgsParser.Issue(3, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE));
    }

    @Test
    public void testSingleEndingSingleQuote() {
        assertResult("foo'", "foo'");
        assertIssues(new ShellArgsParser.Issue(3, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE));
    }

    @Test
    public void testSingleMiddleQuote() {
        assertResult("foo \" bar", "foo", " bar");
        assertIssues(new ShellArgsParser.Issue(4, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testSingleMiddleSingleQuote() {
        assertResult("foo ' bar", "foo", " bar");
        assertIssues(new ShellArgsParser.Issue(4, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testOneQuoteMark() {
        assertResult("\"", "");
        assertIssues(new ShellArgsParser.Issue(0, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testOneSingleQuoteMark() {
        assertResult("'", "");
        assertIssues(new ShellArgsParser.Issue(0, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testThreeQuoteMarks() {
        assertResult("\"\"\"", "");
        assertIssues(new ShellArgsParser.Issue(2, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testThreeSingleQuoteMarks() {
        assertResult("'''", "");
        assertIssues(new ShellArgsParser.Issue(2, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testFourQuoteMarks() {
        assertResult("\"\"\"\"", "", "");
        assertIssues(new ShellArgsParser.Issue(2, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE));
    }

    @Test
    public void testFourSingleQuoteMarks() {
        assertResult("''''", "", "");
        assertIssues(new ShellArgsParser.Issue(2, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE));
    }

    @Test
    public void testAdjacentQuotes() {
        assertResult("\"f\"\"b\"", "f", "b");
        assertIssues(new ShellArgsParser.Issue(3, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE));
    }

    @Test
    public void testAdjacentSingleQuotes() {
        assertResult("'f''b'", "f", "b");
        assertIssues(new ShellArgsParser.Issue(3, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE));
    }

    @Test
    public void testQuoteAdjacentToWord() {
        assertResult("foo\"bar\"", "foo\"bar\"");
        assertIssues(new ShellArgsParser.Issue(3, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE),
                new ShellArgsParser.Issue(7, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE));
    }

    @Test
    public void testSingleQuoteAdjacentToWord() {
        assertResult("foo'bar'", "foo'bar'");
        assertIssues(new ShellArgsParser.Issue(3, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE),
                new ShellArgsParser.Issue(7, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE));
    }

    @Test
    public void testSingleEscapedQuote() {
        assertResult("foo\\\"", "foo\"");
        assertNoIssues();
    }

    @Test
    public void testSingleEscapedSingleQuote() {
        assertResult("foo\\'", "foo'");
        assertNoIssues();
    }

    @Test
    public void testQuoteContainingEscapedQuoteLiteral() {
        assertResult("\"foo \\\" bar\"", "foo \" bar");
        assertNoIssues();
    }

    @Test
    public void testQuoteContainingEscapedQuoteLiteral2() {
        assertResult("\"foo \\\"\"", "foo \"");
        assertNoIssues();
    }

    @Test
    public void testSingleQuoteContainingEscapedSingleQuoteLiteral() {
        assertResult("'foo \\' bar'", "foo ' bar");
        assertNoIssues();
    }

    @Test
    public void testQuoteContainingEscapedSingleQuoteLiteral2() {
        assertResult("\"foo \\\"\"", "foo \"");
        assertNoIssues();
    }

    @Test
    public void testQuoteContainingEscapedSingleSingleQuoteLiteral2() {
        assertResult("'foo \\''", "foo '");
        assertNoIssues();
    }

    @Test
    public void testEscapedQuoteInsideSingleQuotes() {
        assertResult("'foo \\\" bar'", "foo \" bar");
        assertNoIssues();
    }

    @Test
    public void testEscapedSingleQuoteInsideQuotes() {
        assertResult("\"foo \\' bar\"", "foo ' bar");
        assertNoIssues();
    }

    @Test
    public void testQuotedEmptyString() {
        assertResult("\"\"", "");
        assertNoIssues();
    }

    @Test
    public void testSingleQuotedEmptyString() {
        assertResult("''", "");
        assertNoIssues();
    }

    @Test
    public void testQuotedSpacesString() {
        assertResult("\" \"", " ");
        assertNoIssues();
    }

    @Test
    public void testSingleQuotedSpacesString() {
        assertResult("' '", " ");
        assertNoIssues();
    }

    @Test
    public void testSingleAndDoubleQuotes() {
        assertResult("\"foo\" 'bar'", "foo", "bar");
        assertNoIssues();
    }

    @Test
    public void testSingleQuotesWithinDoubleQuotes() {
        assertResult("\"some 'quoted' args\"", "some 'quoted' args");
        assertNoIssues();
    }

    @Test
    public void testDoubleQuotesWithinSingleQuotes() {
        assertResult("'some \"quoted\" args'", "some \"quoted\" args");
        assertNoIssues();
    }

    @Test
    public void testSingleAndDoubleQuotesAdjacent() {
        assertResult("\"foo\"'bar'", "foo", "bar");
        assertIssues(new ShellArgsParser.Issue(5, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE));
    }

    @Test
    public void testMismatchedQuotes() {
        assertResult("\"foo'", "foo'");
        assertIssues(new ShellArgsParser.Issue(0, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE));
    }

    @Test
    public void testSingleCharArgument() {
        assertResult("some-command -f 2", "some-command", "-f", "2");
        assertNoIssues();
    }

    @Test
    public void testSingleCharArgumentAdjoined() {
        assertResult("some-command -f2", "some-command", "-f2");
        assertNoIssues();
    }

    @Test
    public void testSingleCharArgumentWithSingleQuotes() {
        assertResult("some-command -f '2'", "some-command", "-f", "2");
        assertNoIssues();
    }

    @Test
    public void testSingleCharArgumentWithDoubleQuotes() {
        assertResult("some-command -f \"2\"", "some-command", "-f", "2");
        assertNoIssues();
    }

    @Test
    public void testSingleCharArgumentWithExtraWhitespace() {
        assertResult("some-command -f  2", "some-command", "-f", "2");
        assertNoIssues();
    }

    @Test
    public void testSingleCharArgumentWithTrailingWhitespace() {
        assertResult("some-command -f 2 ", "some-command", "-f", "2");
        assertNoIssues();
    }

    @Test
    public void testIssueGetters() {
        ShellArgsParser.Issue issue = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        int columnNumber = issue.getColumnNumber();
        ShellArgsParser.Issue.Type issueType = issue.getType();
        assertEquals(10, columnNumber);
        assertEquals(ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE, issueType);
    }

    @Test
    public void testIssueEquality() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue3 = new ShellArgsParser.Issue(1, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue4 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        assertThat(issue1, is(equalTo(issue2)));
        assertThat(issue1.hashCode(), is(equalTo(issue2.hashCode())));
        assertThat(issue1, not(equalTo(issue3)));
        assertThat(issue1, not(equalTo(issue4)));
    }

    @Test
    public void testIssuesAllIssuesListContents() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issue issue3 = new ShellArgsParser.Issue(20, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);
        issues.addIssue(issue1);
        issues.addIssue(issue2);
        issues.addIssue(issue3);

        assertThat(issues.getAllIssues().size(), is(equalTo(4)));
        assertThat(issues.getAllIssues().size(), is(equalTo(issues.getWarnings().size() + issues.getErrors().size())));

        assertTrue(issues.getAllIssues().contains(issue1));
        assertTrue(issues.getAllIssues().contains(issue2));
        assertTrue(issues.getAllIssues().contains(issue3));
    }

    @Test
    public void testIssuesWarningsListContents() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issue issue3 = new ShellArgsParser.Issue(20, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);
        issues.addIssue(issue1);
        issues.addIssue(issue2);
        issues.addIssue(issue3);

        assertThat(issues.getWarnings().size(), is(equalTo(1)));

        assertFalse(issues.getWarnings().contains(issue1));
        assertTrue(issues.getWarnings().contains(issue2));
        assertFalse(issues.getWarnings().contains(issue3));
    }

    @Test
    public void testIssuesErrorsListContents() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issue issue3 = new ShellArgsParser.Issue(20, ShellArgsParser.Issue.Type.UNMATCHED_QUOTE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);
        issues.addIssue(issue1);
        issues.addIssue(issue2);
        issues.addIssue(issue3);

        assertThat(issues.getErrors().size(), is(equalTo(3)));

        assertTrue(issues.getErrors().contains(issue1));
        assertFalse(issues.getErrors().contains(issue2));
        assertTrue(issues.getErrors().contains(issue3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIssuesAllIssuesListUnmodifiable() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);

        List<ShellArgsParser.Issue> allIssuesList = issues.getAllIssues();
        allIssuesList.add(issue2);
        fail("Should have hit UnsupportedOperationException on add");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIssuesWarningsListUnmodifiable() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);

        List<ShellArgsParser.Issue> warningsList = issues.getWarnings();
        warningsList.add(issue2);
        fail("Should have hit UnsupportedOperationException on add");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIssuesErrorsListUnmodifiable() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);

        List<ShellArgsParser.Issue> errorsList = issues.getErrors();
        errorsList.add(issue2);
        fail("Should have hit UnsupportedOperationException on add");
    }

    @Test
    public void testIssuesFormatterOutputContents() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);
        issues.addIssue(issue2);

        ShellArgsParser.IssuesFormatter formatter = new ShellArgsParser.IssuesFormatter();
        String output = formatter.format(issues);
        assertTrue(output.contains(issue1.toString()));
        assertTrue(output.contains(issue2.toString()));
    }

    @Test
    public void testIssuesFormatterOutputSame() {
        ShellArgsParser.Issue issue1 = new ShellArgsParser.Issue(10, ShellArgsParser.Issue.Type.UNEXPECTED_QUOTE);
        ShellArgsParser.Issue issue2 = new ShellArgsParser.Issue(15, ShellArgsParser.Issue.Type.EXPECTED_WHITESPACE);
        ShellArgsParser.Issues issues = new ShellArgsParser.Issues();
        issues.addIssue(issue1);
        issues.addIssue(issue2);

        ShellArgsParser.IssuesFormatter formatter = new ShellArgsParser.IssuesFormatter();
        String issuesOutput = formatter.format(issues);
        String iterableOutput = formatter.format(issues);
        assertThat(issuesOutput, is(equalTo(iterableOutput)));
    }

    private void assertEmpty(String input) {
        ShellArgsParser sap = new ShellArgsParser(input);
        String[] result = sap.parse();
        issues = sap.getParseIssues();
        assertEquals(Collections.emptyList(), Arrays.asList(result));
    }

    private void assertResult(String input, String ... expecteds) {
        ShellArgsParser sap = new ShellArgsParser(input);
        String[] result = sap.parse();
        issues = sap.getParseIssues();
        assertEquals(Arrays.asList(expecteds), Arrays.asList(result));
    }

    private void assertNoIssues() {
        assertTrue(issues.getAllIssues().isEmpty());
    }

    private void assertIssues(ShellArgsParser.Issue ... expecteds) {
        assertEquals(Arrays.asList(expecteds), issues.getAllIssues());
    }
}
