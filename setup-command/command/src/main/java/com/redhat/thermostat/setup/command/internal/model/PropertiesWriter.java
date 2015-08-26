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

package com.redhat.thermostat.setup.command.internal.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * The Properties.store() method doesn't allow for new lines. This
 * class is used so that when a property key has multiple associated
 * values, they are written in a readable manner.
 */
public class PropertiesWriter extends PrintWriter {

    private static final int INDENT_AMOUNT = 4;

    public PropertiesWriter(OutputStream out) {
        super(out);
    }

    @Override
    public void write(char[] line, int startIdx, int len) {
        for (int i = startIdx; i < len; i++) {
            // interpret new lines as such
            if (isNewLine(line, i)) {
                i++; // skip 'n' in \n
                try {
                    out.write('\\');
                    out.write(System.getProperty("line.separator"));
                    // indent following lines
                    for (int j = 0; j < INDENT_AMOUNT; j++) {
                        out.write(' ');
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            } else {
                try {
                    out.write(line[i]);
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    private boolean isNewLine(char[] line, int j) {
        if (j + 1 > line.length) {
            return false;
        }
        if (line[j] == '\\' && line[j + 1] == 'n') {
            return true;
        }
        return false;
    }
}