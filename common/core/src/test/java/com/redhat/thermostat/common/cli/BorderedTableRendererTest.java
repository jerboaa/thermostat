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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BorderedTableRendererTest {

    private BorderedTableRenderer btr;
    private ByteArrayOutputStream out;

    @Before
    public void setUp() {
        btr = new BorderedTableRenderer(3);
        out = new ByteArrayOutputStream();
    }

    @After
    public void tearDown() {
        out = null;
        btr = null;
    }

    @Test
    public void testHeaderWithBorders() {
        String[] header = {"Foo", "Bar", "Baz"};
        btr.printHeader(header);
        btr.render(out);
        assertEquals("+-----+-----+-----+\n" +
                "| Foo | Bar | Baz |\n" +
                "+-----+-----+-----+\n" +
                "+-----+-----+-----+\n", new String(out.toByteArray()));
    }

    @Test
    public void testRenderTableWithBorders() {
        btr.printHeader("HEADER", "TITLE", "SUBTITLE");
        btr.printLine("hello", "fluff", "world");
        btr.printLine("looooooong", "f1", "foobar");
        btr.printLine("f2", "shoooooooooooort", "poo");
        btr.render(out);
        assertEquals("+------------+------------------+----------+\n" +
                "| HEADER     | TITLE            | SUBTITLE |\n" +
                "+------------+------------------+----------+\n" +
                "| hello      | fluff            | world    |\n" +
                "| looooooong | f1               | foobar   |\n" +
                "| f2         | shoooooooooooort | poo      |\n" +
                "+------------+------------------+----------+\n", new String(out.toByteArray()));
    }

    @Test
    public void testRenderTableContinuous() {
        btr.printHeader("HEADER", "TITLE", "SUBTITLE");
        btr.printLine("hello", "fluff", "world");
        btr.printLine("looooooong", "f1", "foobar");
        btr.printLine("f2", "shoooooooooooort", "poo");
        btr.render(out);
        assertEquals("+------------+------------------+----------+\n" +
                "| HEADER     | TITLE            | SUBTITLE |\n" +
                "+------------+------------------+----------+\n" +
                "| hello      | fluff            | world    |\n" +
                "| looooooong | f1               | foobar   |\n" +
                "| f2         | shoooooooooooort | poo      |\n" +
                "+------------+------------------+----------+\n", new String(out.toByteArray()));
        btr.printLine("newwwwwwwwwwww", "line", "added");
        btr.render(out = new ByteArrayOutputStream());
        assertEquals("+----------------+------------------+----------+\n" +
                "| HEADER         | TITLE            | SUBTITLE |\n" +
                "+----------------+------------------+----------+\n" +
                "| hello          | fluff            | world    |\n" +
                "| looooooong     | f1               | foobar   |\n" +
                "| f2             | shoooooooooooort | poo      |\n" +
                "| newwwwwwwwwwww | line             | added    |\n" +
                "+----------------+------------------+----------+\n", new String(out.toByteArray()));
    }
}