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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.shared.locale.Translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Parser for thermostat shell command line input. Splits lines on whitespaces to chunk commands and arguments,
 * respecting quotation marks, so that ex:
 *
 * some-command --flag "quoted arg"
 *
 * is split into three parts: "some-command", "--flag", "quoted arg"
 */
class ShellArgsParser {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int NOT_YET_PARSED = -1;
    private final String input;
    private int pos = NOT_YET_PARSED;
    private char c;
    private final Issues issues = new Issues();

    ShellArgsParser(String input) {
        this.input = input;
    }

    String[] parse() {
        if (input.isEmpty()) {
            ++pos;
            return new String[]{};
        }
        readChar();
        List<String> result = new ArrayList<>();
        do {
            consumeWhitespace();
            if (isQuote()) {
                result.add(quote());
            } else if (isWord()) {
                result.add(word());
            }
        } while (ready());
        return result.toArray(new String[result.size()]);
    }

    Issues getParseIssues() {
        if (pos == NOT_YET_PARSED) {
            throw new IllegalStateException(translator.localize(LocaleResources.PARSE_ISSUES_CALLED_BEFORE_PARSE).getContents());
        }
        return issues;
    }

    private void consumeWhitespace() {
        while (isWhitespace() && ready()) {
            readChar();
        }
    }

    private String quote() {
        StringBuilder sb = new StringBuilder();
        boolean closed = false;
        int startPos = pos;
        char openingQuote = c;
        while (ready()) {
            readChar();
            if (isEscapedQuote()) {
                readChar();
                sb.append(c);
                continue;
            }
            if (c == openingQuote) {
                if (ready()) {
                    readChar();
                    if (!ready()) {
                        if (isQuote()) {
                            issues.addIssue(new Issue(pos, Issue.Type.UNMATCHED_QUOTE));
                        }
                    } else {
                        if (!isWhitespace()) {
                            issues.addIssue(new Issue(pos, Issue.Type.EXPECTED_WHITESPACE));
                        }
                    }
                }
                closed = true;
                break;
            }
            sb.append(c);
        }
        if (!closed) {
            issues.addIssue(new Issue(startPos, Issue.Type.UNMATCHED_QUOTE));
        }
        return sb.toString();
    }

    private String word() {
        StringBuilder sb = new StringBuilder();
        sb.append(c);
        while (!isWhitespace() && ready()) {
            readChar();
            if (isEscapedQuote()) {
                readChar();
                sb.append(c);
                continue;
            }
            if (!isWhitespace()) {
                sb.append(c);
            }
            if (isQuote()) {
                issues.addIssue(new Issue(pos, Issue.Type.UNEXPECTED_QUOTE));
            }
        }
        return sb.toString();
    }

    private boolean ready() {
        return pos < input.length() - 1;
    }

    private char readChar() {
        c = input.charAt(++pos);
        return c;
    }

    private char lookahead() {
        return input.charAt(pos + 1);
    }

    private boolean isEscapedQuote() {
        return c == '\\' && (lookahead() == '"' || lookahead() == '\'');
    }

    private boolean isQuote() {
        return c == '"' || c == '\'';
    }

    private boolean isWhitespace() {
        return Character.isWhitespace(c);
    }

    private boolean isWord() {
        return !(isQuote() || isWhitespace());
    }

    static class Issues {
        private final List<Issue> issues = new ArrayList<>();

        void addIssue(Issue issue) {
            this.issues.add(requireNonNull(issue));
        }

        List<Issue> getAllIssues() {
            return Collections.unmodifiableList(issues);
        }

        List<Issue> getWarnings() {
            return Collections.unmodifiableList(filterIssuesBySeverity(getAllIssues(), Issue.Type.Severity.WARN));
        }

        List<Issue> getErrors() {
            return Collections.unmodifiableList(filterIssuesBySeverity(getAllIssues(), Issue.Type.Severity.ERROR));
        }

        private static List<Issue> filterIssuesBySeverity(Iterable<Issue> issues, Issue.Type.Severity severity) {
            List<Issue> results = new ArrayList<>();
            for (Issue issue : issues) {
                if (issue.getType().getSeverity().equals(severity)) {
                    results.add(issue);
                }
            }
            return results;
        }
    }

    static class Issue {
        private final int columnNumber;
        private final Type type;

        Issue(int columnNumber, Type type) {
            this.columnNumber = columnNumber;
            this.type = requireNonNull(type);
        }

        int getColumnNumber() {
            return columnNumber;
        }

        Type getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Issue issue = (Issue) o;

            return columnNumber == issue.columnNumber && type == issue.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(columnNumber, type);
        }

        @Override
        public String toString() {
            return type.getSeverity() + " : " + type.toString() + " col " + Integer.toString(columnNumber);
        }

        enum Type {
            UNMATCHED_QUOTE(Severity.ERROR),
            UNEXPECTED_QUOTE(Severity.ERROR),
            EXPECTED_WHITESPACE(Severity.WARN),
            ;

            private final Severity severity;

            Type(Severity severity) {
                this.severity = requireNonNull(severity);
            }

            public Severity getSeverity() {
                return severity;
            }

            enum Severity {
                WARN,
                ERROR,
            }
        }
    }

    static class IssuesFormatter {
        String format(Issues issues) {
            return format(issues.getAllIssues());
        }

        String format(Iterable<Issue> issues) {
            StringBuilder sb = new StringBuilder();
            for (Issue issue : issues) {
                sb.append(issue.toString()).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

}
