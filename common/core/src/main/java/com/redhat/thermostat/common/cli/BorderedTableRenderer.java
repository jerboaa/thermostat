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

package com.redhat.thermostat.common.cli;

import java.io.PrintStream;

import com.redhat.thermostat.common.utils.StringUtils;

/**
 * A {@link TableRenderer} that adds column and row dividers
 * to text-based tables
 */
public class BorderedTableRenderer extends TableRenderer {

    private StringBuilder format;

    public BorderedTableRenderer(int numColumns) {
        super(numColumns);
    }

    public BorderedTableRenderer(int numColumns, int minWidth) {
        super(numColumns, minWidth);
    }

    /* can input an array with a single hyphen at each index to neatly print a divider */
    private boolean checkForDivider(String[] line) {
        String prev = "-";
        for(int i = 0; i < line.length; i++) {
            if (!line[i].equals(prev)) {
                return false;
            }
            prev = line[i];
        }
        return true;
    }

    @Override
    public void render(PrintStream out) {
        printHeaderWithBorders(out, header);
        sortLines();
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            if (checkForDivider(line)) {
                if (i != lines.size() - 1) {
                    printDivider(out);
                }
            } else {
                renderLineWithBorders(out, line);
            }
        }
        printDivider(out);
    }

    /* prints horizontal line dividers to separate header from table entries */
    private void printDivider(PrintStream out) {
        StringBuilder divider = new StringBuilder();
        divider.append("+");
        for (int i = 0; i < numColumns; i++) {
            int width = maxColumnWidths[i];
            String dashes = StringUtils.repeat("-", width + 2);
            divider.append(dashes + "+");
        }
        divider.append("\n");
        out.print(divider.toString());
    }

    private void printHeaderWithBorders(PrintStream out, String[] header) {
        format = new StringBuilder();
        for (int i = 0; i < numColumns; i++) {
            int width = maxColumnWidths[i];
            String dashes = StringUtils.repeat("-", width + 2);
            format.append("| %-" + width + "s ");
        }
        format.append("|%n");
        printDivider(out);
        if (!(header == null)) {
            out.printf(format.toString(), header);
            printDivider(out);
        }
    }

    private void renderLineWithBorders(PrintStream out, String... line) {
        out.printf(format.toString(), line);
    }

}
