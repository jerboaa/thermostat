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

package com.redhat.thermostat.common.cli;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TableRenderer {

    private List<String[]> lines;
    private String[] header;
    private List<Integer> columnSortingQueue = new ArrayList<>();
    private int[] maxColumnWidths;
    private int lastPrintedLine = -1;

    private int numColumns;
    private int minWidth;

    public TableRenderer(int numColumns) {
        this(numColumns, 1);
    }

    public TableRenderer(int numColumns, int minWidth) {
        this.numColumns = numColumns;
        lines = new ArrayList<>();
        maxColumnWidths = new int[numColumns];
        this.minWidth = minWidth;
    }

    public void printLine(String... line) {
        checkLine(line);
        lines.add(line);
    }

    public void printHeader(String... line) {
        checkLine(line);
        this.header = line;
    }

    private void checkLine(final String[] line) {
        if (line.length != numColumns) {
            throw new IllegalArgumentException("Invalid number of columns: " + line.length + ", expected: " + numColumns);
        }
        for (int i = 0; i < numColumns; i++) {
            maxColumnWidths[i] = Math.max(Math.max(maxColumnWidths[i], line[i].length()), minWidth);
        }
    }

    public void render(OutputStream os) {
        PrintStream out = new PrintStream(os);
        render(out);
    }

    public void render(PrintStream out) {
        if (lastPrintedLine == -1 && header != null) {
            renderLine(out, header);
        }
        sortLines();
        for (int i = lastPrintedLine + 1; i < lines.size(); i++) {
            String[] line = lines.get(i);
            renderLine(out, line);
            lastPrintedLine = i;
        }
    }

    private void sortLines() {
        Collections.sort(lines, new Comparator<String[]>() {
            @Override
            public int compare(final String[] lines1, final String[] lines2) {
                int comparison = 0;
                for (Integer column : columnSortingQueue) {
                    comparison = lines1[column].compareTo(lines2[column]);
                    if (comparison != 0) {
                        break;
                    }
                }
                return comparison;
            }
        });
    }

    public void sortByColumn(int column) {
        if (column < numColumns) {
            columnSortingQueue.add(column);
        } else {
            throw new IllegalArgumentException("Invalid number of columns: " + column + ", expected: " + numColumns);
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

