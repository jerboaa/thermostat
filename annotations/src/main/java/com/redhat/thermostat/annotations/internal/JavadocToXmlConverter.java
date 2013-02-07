/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

/**
 * Converts javadoc comments into valid html-like xml.
 * <p>
 * Used to generate documentation.
 */
class JavadocToXmlConverter {

    // TODO get rid of this once we use JEP 106

    /**
     * Perform an ad-hoc set of conversions to convert the javadoc input string
     * into something that is valid xml and can be used in html documents
     * safely.
     */
    public String convert(String input) {
        String result = input;

        // @see Foo -> See Also: <code>Foo</code>
        result = result.replaceAll("@see (.+?)(\n|$)", "See Also: <code>$1</code><p>\n");

        // <p> is valid html but bad xml (without a </p>)
        // convert it to 2 <br />'s so we get one blank line
        result = result.replace("<p>", "<br /> <br />");
        result = result.replace("</p>", "");

        // {@code foobar} -> <code>foobar</code>
        result = result.replaceAll("\\{@code (.*?)\\}", "<code>$1</code>");

        // {@link foobar} -> <code>foobar</code>
        result = result.replaceAll("\\{@link (.*?)\\}", "<code>$1</code>");

        // Foo#bar(Baz) -> Foo.bar(Baz)
        result = result.replaceAll("(\\w+)#(\\w+)", "$1.$2");

        // <h1>Foo</h1> -> <h4>Foo</h4>
        int offset = 4;
        for (int i = 1; i < offset; i++) {
            result = convertHeadingLevel(result, i, i + offset);
        }


        return result;
    }

    private String convertHeadingLevel(String replace, int source, int target) {
        return replace
                .replaceAll("<h" + source + ">", "<h" + target + ">")
                .replaceAll("</h" + source + ">", "</h" + target + ">");
    }
}
