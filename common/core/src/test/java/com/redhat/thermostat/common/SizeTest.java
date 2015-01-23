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

package com.redhat.thermostat.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.Size.Unit;

public class SizeTest {
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
    public void testAccessorMethods() {
        Size size = new Size(1, Size.Unit.B);
        assertEquals(1, size.getValue(), 0.00001);
        assertEquals(Size.Unit.B, size.getUnit());
    }

    @Test
    public void testHashing() {
        Size size1 = new Size(1024, Size.Unit.B);
        Size size2 = new Size(1, Size.Unit.B);

        assertTrue(size1.hashCode() != size2.hashCode());
    }

    @Test
    public void testEquality() {
        Size size1 = new Size(1024, Size.Unit.B);
        Size size2 = new Size(1, Size.Unit.KiB);
        Size size3 = new Size(2, Size.Unit.KiB);

        assertEquals(size1, size2);
        assertEquals(size2, size1);
        assertFalse(size1.equals(size3));

        assertFalse(size1.equals(null));
        assertFalse(size1.equals(new Object()));
    }

    @Test
    public void testUnitConversion() {
        Size size = new Size(1024, Size.Unit.KiB);

        Size sizeInBytes = size.convertTo(Size.Unit.B);
        assertEquals(1024 * 1024, sizeInBytes.getValue(), 0.001);
        assertEquals(Size.Unit.B, sizeInBytes.getUnit());

        Size sizeInMegaBytes = size.convertTo(Size.Unit.MiB);
        assertEquals(1, sizeInMegaBytes.getValue(), 0.001);
        assertEquals(Size.Unit.MiB, sizeInMegaBytes.getUnit());

        assertEquals(size, sizeInBytes);
        assertEquals(size, sizeInMegaBytes);

    }

    @Test
    public void testToString() {
        assertEquals("1 B", Size.bytes(1).toString());
        assertEquals("1023 B", Size.bytes(1023).toString());
        assertEquals("1.0 KiB", Size.bytes(1024).toString());
        assertEquals("1024.0 KiB", Size.bytes(1_048_575).toString());
        assertEquals("1.0 MiB", Size.bytes(1_048_576).toString());
        assertEquals("10.0 MiB", Size.bytes(10_480_000).toString());
        assertEquals("42.0 MiB", Size.bytes(44_040_000).toString());
        assertEquals("99.9 MiB", Size.bytes(104_752_742).toString());
        assertEquals("100.0 MiB", Size.bytes(104_857_600).toString());
        assertEquals("500.0 MiB", Size.bytes(524_288_000).toString());
        assertEquals("900.0 MiB", Size.bytes(943_718_400).toString());
        assertEquals("999.9 MiB", Size.bytes(1_048_471_000).toString());
        assertEquals("1.0 GiB", Size.bytes(1_073_741_824).toString());
        assertEquals("1.1 GiB", Size.bytes(1_181_116_000).toString());
        assertEquals("9.9 GiB", Size.bytes(10_630_044_000l).toString());
        assertEquals("99.9 GiB", Size.bytes(107_266_808_000l).toString());
        assertEquals("1.0 TiB", Size.bytes(1_099_511_627_776l).toString());
    }

    @Test
    public void testParsingNumber() {
        assertEquals(Size.bytes(1), Size.parse("1"));
    }

    @Test
    public void testParsingNumberAndUnit() {
        assertEquals(Size.bytes(1), Size.parse("1 B"));

        assertEquals(new Size(1024, Unit.KiB), Size.parse("1024.0 KiB"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsingString() {
        Size.parse("B");
    }

    @Test
    public void testGetBestUnit() {
        assertEquals(Size.Unit.KiB, Size.Unit.getBestUnit(1024));
        assertEquals(Size.Unit.B, Size.Unit.getBestUnit(999));

        assertEquals(Size.Unit.MiB, Size.Unit.getBestUnit(943_718_400));
        assertEquals(Size.Unit.MiB, Size.Unit.getBestUnit(1_048_471_000));
        assertEquals(Size.Unit.GiB, Size.Unit.getBestUnit(1_073_741_824));
        assertEquals(Size.Unit.GiB, Size.Unit.getBestUnit(107_266_808_000l));

        assertEquals(Size.Unit.TiB, Size.Unit.getBestUnit(1_099_511_627_776l));
    }

}

