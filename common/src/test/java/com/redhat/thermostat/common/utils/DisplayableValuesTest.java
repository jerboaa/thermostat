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

package com.redhat.thermostat.common.utils;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.utils.DisplayableValues.Scale;

public class DisplayableValuesTest {

    private static Locale defaultLocale;

    @BeforeClass
    public static void setUp() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testBytes() {
        testBytesOutput("1", "B", DisplayableValues.bytes(1));
        testBytesOutput("1023", "B", DisplayableValues.bytes(1023));
        testBytesOutput("1.0", "KiB", DisplayableValues.bytes(1024));
        testBytesOutput("1024.0", "KiB", DisplayableValues.bytes(1_048_575));
        testBytesOutput("1.0", "MiB", DisplayableValues.bytes(1_048_576));
        testBytesOutput("10.0", "MiB", DisplayableValues.bytes(10_480_000));
        testBytesOutput("42.0", "MiB", DisplayableValues.bytes(44_040_000));
        testBytesOutput("99.9", "MiB", DisplayableValues.bytes(104_752_742));
        testBytesOutput("100.0", "MiB", DisplayableValues.bytes(104_857_600));
        testBytesOutput("500.0", "MiB", DisplayableValues.bytes(524_288_000));
        testBytesOutput("900.0", "MiB", DisplayableValues.bytes(943_718_400));
        testBytesOutput("999.9", "MiB", DisplayableValues.bytes(1_048_471_000));
        testBytesOutput("1.0", "GiB", DisplayableValues.bytes(1_073_741_824));
        testBytesOutput("1.1", "GiB", DisplayableValues.bytes(1_181_116_000));
        testBytesOutput("9.9", "GiB", DisplayableValues.bytes(10_630_044_000l));
        testBytesOutput("99.9", "GiB", DisplayableValues.bytes(107_266_808_000l));
        testBytesOutput("1.0", "TiB", DisplayableValues.bytes(1_099_511_627_776l));
    }

    private void testBytesOutput(String number, String units, String[] output) {
        assertEquals(2, output.length);
        assertEquals(number, output[0]);
        assertEquals(units, output[1]);
    }
    
    @Test
    public void testScales() {
        
        double value = Scale.convertTo(Scale.KiB, 1024);
        assertEquals(1, value, 0);
        
        value = Scale.convertTo(Scale.KiB, 2048);
        assertEquals(2, value, 0);
        
        value = Scale.convertTo(Scale.KiB, 524_288);
        assertEquals(512, value, 0);
        
        value = Scale.convertTo(Scale.MiB, 524_288_000);
        assertEquals(500, value, 0);
    }
}
