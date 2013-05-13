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

package com.redhat.thermostat.common.locale;

import static org.junit.Assert.assertEquals;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.junit.Test;

public class TranslateTest {

    enum TestStrings {
        THE_STRING,
        ;
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
        Translate<TestStrings> translate = getTranslator(TestStrings.THE_STRING.name(), "Localized String");

        assertEquals("Localized String", translate.localize(TestStrings.THE_STRING).getContents());
    }

    @Test
    public void testLocalizeWithArguments() {
        Translate<TestStrings> translate = getTranslator(TestStrings.THE_STRING.name(), "Parameter: {0}");

        assertEquals("Parameter: FOO", translate.localize(TestStrings.THE_STRING, "FOO").getContents());
    }

    @Test
    public void testLocalizeWithSeveralArguments() {
        Translate<TestStrings> translate = getTranslator(TestStrings.THE_STRING.name(), "Parameter1: {0}  Parameter2: {1}  Parameter3: {2}");

        assertEquals("Parameter1: ONE  Parameter2: TWO  Parameter3: THREE", translate.localize(TestStrings.THE_STRING, "ONE", "TWO", "THREE").getContents());
    }

    @Test
    public void testLocalizeWithSpecialList() {
        Translate<TestStrings> translate = getTranslator(TestStrings.THE_STRING.name(), "Parameter1: {0}  ParameterList: {1}  Parameter2: {2}");

        assertEquals("Parameter1: ONE  ParameterList: FOO, BAR, BAZ  Parameter2: TWO", translate.localize(TestStrings.THE_STRING, new String[]{"FOO", "BAR", "BAZ"}, ", ", 1, "ONE", "TWO").getContents());
       
    }

    private Translate<TestStrings> getTranslator(String key, String localizedString) {
        ResourceBundle resources = new LocalizedResourceBundle(key, localizedString);
        return new Translate<>(resources, TestStrings.class);
    }
}

