/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.tools.cli;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

class TableRenderer {

    private List<String[]> lines;
    private int[] maxColumnWidths;

    private int numColumns;

    TableRenderer(int numColumns) {
        this.numColumns = numColumns;
        lines = new ArrayList<>();
        maxColumnWidths = new int[numColumns];
    }

    void printLine(String... line) {
        if (line.length != numColumns) {
            throw new IllegalArgumentException("Invalid number of columns: " + line.length + ", expected: " + numColumns);
        }
        lines.add(line);
        for (int i = 0; i < numColumns; i++) {
            maxColumnWidths[i] = Math.max(maxColumnWidths[i], line[i].length());
        }
    }

    void render(OutputStream os) {
        PrintStream out = new PrintStream(os);
        render(out);
    }

    void render(PrintStream out) {
        for (String[] line : lines) {
            renderLine(out, line);
        }
    }

    private void renderLine(PrintStream out, String[] line) {
        for (int i = 0; i < numColumns; i++) {
            out.print(line[i]);
            padOrNewline(out, line, i);
            
        }
    }

    private void padOrNewline(PrintStream out, String[] line, int i) {
        if (i < numColumns - 1) {
            int pad = maxColumnWidths[i] - line[i].length() + 1;
            fillSpaces(out, pad);
        } else {
            out.println();
        }
    }

    private void fillSpaces(PrintStream out, int pad) {
        for (int i = 0; i < pad; i++) {
            out.print(" ");
        }
    }

}
