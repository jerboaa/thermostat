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

package com.redhat.thermostat.annotations.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class JavadocToXmlConverterTest {

    private JavadocToXmlConverter converter;

    @Before
    public void setUp() {
        converter = new JavadocToXmlConverter();
    }

    @Test
    public void testParagraphs() {
        String input = "" +
                "Foo bar baz\n" +
                "<p>\n" +
                "frob\n";

        String expected = "" +
                "Foo bar baz\n" +
                "<br /> <br />\n" +
                "frob\n";

        assertEquals(expected, converter.convert(input));
    }

    @Test
    public void testCodeSegments() {
        String input = "" +
                "Foo bar baz\n" +
                "<p>\n" +
                "frob\n";

        String expected = "" +
                "Foo bar baz\n" +
                "<br /> <br />\n" +
                "frob\n";

        assertEquals(expected, converter.convert(input));
    }

    @Test
    public void testHeadingConversion() {
        String input = "" +
                "<h1>test</h1>\n" +
                "frob\n";

        String expected = "" +
                "<h5>test</h5>\n" +
                "frob\n";

        assertEquals(expected, converter.convert(input));
    }

    @Test
    public void testSeeAlsoSections() {
        String input = "" +
                " foo\n" +
                " @see Foo#bar(baz)\n" +
                " @see Spam#EGGS";

        String expected = "" +
                " foo\n" +
                " See Also: <code>Foo.bar(baz)</code><br /> <br />\n" +
                " See Also: <code>Spam.EGGS</code><br /> <br />\n";

        assertEquals(expected, converter.convert(input));
    }

    @Test
    public void testComplexJavadoc() {
        String input = "" +
                " foo \n" +
                " bar.\n" +
                " <p>\n" +
                " baz (eggs) spam \n" +
                " {@link BundleContext#getService(ServiceReference)} or\n" +
                " {@link OSGIUtils#getService(Class)}.\n" +
                " <p>\n" +
                " ham\n" +
                " \n";

        String expected = "" +
                " foo \n" +
                " bar.\n" +
                " <br /> <br />\n" +
                " baz (eggs) spam \n" +
                " <code>BundleContext.getService(ServiceReference)</code> or\n" +
                " <code>OSGIUtils.getService(Class)</code>.\n" +
                " <br /> <br />\n" +
                " ham\n" +
                " \n";

        assertEquals(expected, converter.convert(input));
    }

}

