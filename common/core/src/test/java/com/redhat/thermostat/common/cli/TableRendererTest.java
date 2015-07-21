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

package com.redhat.thermostat.common.cli;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TableRendererTest {

    private TableRenderer tableRenderer;
    private ByteArrayOutputStream out;

    @Before
    public void setUp() {
        tableRenderer = new TableRenderer(3);
        out = new ByteArrayOutputStream();
    }

    @After
    public void tearDown() {
        out = null;
        tableRenderer = null;
    }

    @Test
    public void testSingleLine() {
        tableRenderer.printLine("hello", "fluff", "world");
        tableRenderer.render(out);
        assertEquals("hello fluff world\n", new String(out.toByteArray()));
    }

    @Test
    public void testMultiLine() {
        tableRenderer.printLine("hello", "fluff", "world");
        tableRenderer.printLine("looooooong", "f1", "foobar");
        tableRenderer.printLine("f2", "shoooooooooooort", "poo");
        tableRenderer.render(out);
        assertEquals("hello      fluff            world\n" +
                     "looooooong f1               foobar\n" +
                     "f2         shoooooooooooort poo\n", new String(out.toByteArray()));
    }

    @Test
    public void testMultiLineContinuous() {
        tableRenderer.printHeader("TITLE", "TIME", "PLACE");
        tableRenderer.printLine("hello", "fluff", "world");
        tableRenderer.printLine("looooooong", "f1", "foobar");
        tableRenderer.printLine("f2", "shoooooooooooort", "poo");
        tableRenderer.render(out);
        assertEquals("TITLE      TIME             PLACE\n" +
                     "hello      fluff            world\n" +
                     "looooooong f1               foobar\n" +
                     "f2         shoooooooooooort poo\n", new String(out.toByteArray()));
        tableRenderer.printLine("f3", "foobar", "poo");
        tableRenderer.render(out);
        assertEquals("TITLE      TIME             PLACE\n" +
                     "hello      fluff            world\n" +
                     "looooooong f1               foobar\n" +
                     "f2         shoooooooooooort poo\n" +
                     "f3         foobar           poo\n", new String(out.toByteArray()));
    }

    @Test
    public void testMultiLineSorting() {
        tableRenderer.sortByColumn(0);
        tableRenderer.sortByColumn(2);
        tableRenderer.sortByColumn(1);
        tableRenderer.printLine("animal", "brown", "bear");
        tableRenderer.printLine("animal", "black", "bear");
        tableRenderer.printLine("animal", "grey", "rhino");
        tableRenderer.printLine("animal", "aqua-green", "turtle");
        tableRenderer.printLine("animal", "polar", "bear");
        tableRenderer.printLine("animal", "green", "alligator");
        tableRenderer.printLine("animal", "white", "rabbit");
        tableRenderer.printLine("animal", "brown", "alligator");
        tableRenderer.printLine("animal", "brown", "rabbit");
        tableRenderer.printLine("animal", "brown", "rat");
        tableRenderer.printLine("bird", "red", "parrot");
        tableRenderer.printLine("bird", "green", "parrot");
        tableRenderer.printLine("bird", "blue", "parrot");
        tableRenderer.printLine("bird", "red", "ostrich");
        tableRenderer.printLine("bird", "yellow", "rooster");
        tableRenderer.printLine("bird", "tuxedo", "penguin");
        tableRenderer.printLine("fish", "yellow", "trout");
        tableRenderer.printLine("fish", "rainbow", "trout");
        tableRenderer.printLine("fish", "golden", "trout");
        tableRenderer.printLine("fish", "tiger", "trout");

        tableRenderer.render(out);
        assertEquals("animal brown      alligator\n" +
                     "animal green      alligator\n" +
                     "animal black      bear\n" +
                     "animal brown      bear\n" +
                     "animal polar      bear\n" +
                     "animal brown      rabbit\n" +
                     "animal white      rabbit\n" +
                     "animal brown      rat\n" +
                     "animal grey       rhino\n" +
                     "animal aqua-green turtle\n" +
                     "bird   red        ostrich\n" +
                     "bird   blue       parrot\n" +
                     "bird   green      parrot\n" +
                     "bird   red        parrot\n" +
                     "bird   tuxedo     penguin\n" +
                     "bird   yellow     rooster\n" +
                     "fish   golden     trout\n" +
                     "fish   rainbow    trout\n" +
                     "fish   tiger      trout\n" +
                     "fish   yellow     trout\n", new String(out.toByteArray()));
    }

    @Test
    public void testHeader() {
        tableRenderer.printHeader("HEADER", "TITLE", "SUBTITLE");
        tableRenderer.render(out);
        assertEquals("HEADER TITLE SUBTITLE\n", new String(out.toByteArray()));
    }

    @Test
    public void testHeaderWithLine() {
        tableRenderer.printHeader("HEADER", "TITLE", "SUBTITLE");
        tableRenderer.printLine("hello", "foo", "subtitle");
        tableRenderer.render(out);
        assertEquals("HEADER TITLE SUBTITLE\n" +
                     "hello  foo   subtitle\n", new String(out.toByteArray()));
    }

    @Test
    public void testHeaderNotSet() {
        tableRenderer.printLine("No", "Header", "Present");
        tableRenderer.render(out);
        assertEquals("No Header Present\n", new String(out.toByteArray()));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMultiLineSortingWithColumnsOutOfRange() {
        tableRenderer.sortByColumn(3);
        tableRenderer.sortByColumn(7);
        tableRenderer.sortByColumn(1);
        tableRenderer.printLine("animal", "brown", "bear");
        tableRenderer.printLine("animal", "black", "bear");
        tableRenderer.printLine("animal", "grey", "rhino");
        tableRenderer.printLine("animal", "aqua-green", "turtle");
        tableRenderer.printLine("animal", "polar", "bear");
        tableRenderer.printLine("animal", "green", "alligator");
        tableRenderer.printLine("animal", "white", "rabbit");
        tableRenderer.printLine("animal", "brown", "alligator");
        tableRenderer.printLine("animal", "brown", "rabbit");
        tableRenderer.printLine("animal", "brown", "rat");
        tableRenderer.printLine("bird", "red", "parrot");
        tableRenderer.printLine("bird", "green", "parrot");
        tableRenderer.printLine("bird", "blue", "parrot");
        tableRenderer.printLine("bird", "red", "ostrich");
        tableRenderer.printLine("bird", "yellow", "rooster");
        tableRenderer.printLine("bird", "tuxedo", "penguin");
        tableRenderer.printLine("fish", "yellow", "trout");
        tableRenderer.printLine("fish", "rainbow", "trout");
        tableRenderer.printLine("fish", "golden", "trout");
        tableRenderer.printLine("fish", "tiger", "trout");

        tableRenderer.render(out);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidLine() {
        tableRenderer.printLine("hello", "fluff", "world", "boom");
    }
}

