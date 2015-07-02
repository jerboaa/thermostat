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

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for thermostat shell command line input. Splits lines on whitespaces to chunk commands and arguments,
 * respecting quotation marks, so that ex:
 *
 * some-command --flag "quoted arg"
 *
 * is split into three parts: "some-command", "--flag", "quoted arg"
 */
class ShellArgsParser {

    private final String input;
    private int pos = 0;
    private char c;

    ShellArgsParser(String input) {
        this.input = input;
        if (input.length() > 0) {
            c = input.charAt(pos);
        }
    }

    String[] parse() {
        if (input.isEmpty()) {
            return new String[]{};
        }
        List<String> result = new ArrayList<>();
        while (ready()) {
            if (isWhitespace()) {
                whitespace();
            } else if (isQuote()) {
                result.add(quote());
            } else {
                result.add(word());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private void whitespace() {
        while (isWhitespace() && ready()) {
            readChar();
        }
    }

    private String quote() {
        StringBuilder sb = new StringBuilder();
        while (ready()) {
            readChar();
            if (isEscapedQuote()) {
                readChar();
                sb.append(c);
                continue;
            }
            if (isQuote()) {
                break;
            }
            sb.append(c);
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
        return c == '\\' && lookahead() == '"';
    }

    private boolean isQuote() {
        return c == '"';
    }

    private boolean isWhitespace() {
        return Character.isWhitespace(c);
    }

}
