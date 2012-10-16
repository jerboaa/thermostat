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

package com.redhat.thermostat.common.locale;

import static org.junit.Assert.assertEquals;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.junit.Test;

public class TranslateTest {

    enum TestStrings {
        SIMPLE_STRING,
        STRING_WITH_PARAMETER,
    }

    // Mockito can't mock the final method getMessage() which is what Translate
    // uses. Create a mock the old-fashioned way.
    private static class LocalizedResourceBundle extends ListResourceBundle {

        private final Object[] contents;

        public LocalizedResourceBundle(String key, String localizedString) {
            contents = new Object[] { key, localizedString };
        }

        @Override
        protected Object[][] getContents() {
            return new Object[][] { contents };
        }
    }

    @Test
    public void testLocalizeWithoutArguments() {
        ResourceBundle resources = new LocalizedResourceBundle(TestStrings.SIMPLE_STRING.name(), "Localized String");

        Translate<TestStrings> translate = new Translate<>(resources, TestStrings.class);

        assertEquals("Localized String", translate.localize(TestStrings.SIMPLE_STRING));
    }

    @Test
    public void testLocalizeWithArguments() {
        ResourceBundle resources = new LocalizedResourceBundle(TestStrings.STRING_WITH_PARAMETER.name(), "Parameter: {0}");

        Translate<TestStrings> translate = new Translate<>(resources, TestStrings.class);

        assertEquals("Parameter: FOO", translate.localize(TestStrings.STRING_WITH_PARAMETER, "FOO"));

    }
}
