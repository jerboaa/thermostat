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

package com.redhat.thermostat.common.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimpleArgumentSpecTest {

    private SimpleArgumentSpec spec;
    private SimpleArgumentSpec other;

    @Before
    public void setUp() {
        spec = new SimpleArgumentSpec();
        other = new SimpleArgumentSpec();
    }

    @After
    public void tearDown() {
        spec = null;
        other = null;
    }

    @Test
    public void testName() {
        spec.setName("test1");
        assertEquals("test1", spec.getName());
        spec.setName("test2");
        assertEquals("test2", spec.getName());
    }

    @Test
    public void testDescription() {
        spec.setDescription("test1");
        assertEquals("test1", spec.getDescription());
        spec.setDescription("test2");
        assertEquals("test2", spec.getDescription());
    }

    @Test
    public void testRequired() {
        spec.setRequired(true);
        assertTrue(spec.isRequired());
        spec.setRequired(false);
        assertFalse(spec.isRequired());
    }

    @Test
    public void testUsesAdditionalArgument() {
        spec.setUsingAdditionalArgument(true);
        assertTrue(spec.isUsingAdditionalArgument());
        spec.setUsingAdditionalArgument(false);
        assertFalse(spec.isUsingAdditionalArgument());
    }

    @Test
    public void testShortOption() {
        spec.setShortOption("test1");
        assertEquals("test1", spec.getShortOption());
        spec.setShortOption("test2");
        assertEquals("test2", spec.getShortOption());
    }

    @Test
    public void testEquals() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        assertTrue(spec.equals(other));
    }

    @Test
    public void testEqualsUnequalName() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        other.setName("fluff");

        assertFalse(spec.equals(other));
    }

    @Test
    public void testEqualsUnequalDescription() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        other.setDescription("fluff");

        assertFalse(spec.equals(other));
    }

    @Test
    public void testEqualsUnequalUsingAdditionalArgument() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        other.setUsingAdditionalArgument(false);

        assertFalse(spec.equals(other));
    }

    @Test
    public void testEqualsUnequalRequired() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        other.setRequired(false);

        assertFalse(spec.equals(other));
    }

    @Test
    public void testEqualsUnequalShortOption() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        other.setShortOption("fluff");

        assertFalse(spec.equals(other));
    }

    @Test
    public void testEqualsNull() {
        prepareSpecForEqualsTest(spec);

        assertFalse(spec.equals(null));
    }

    @Test
    public void testHashCode() {
        prepareSpecForEqualsTest(spec);
        prepareSpecForEqualsTest(other);

        assertEquals(spec.hashCode(), other.hashCode());
    }

    private void prepareSpecForEqualsTest(SimpleArgumentSpec spec) {
        spec.setName("test");
        spec.setDescription("description");
        spec.setUsingAdditionalArgument(true);
        spec.setRequired(true);
        spec.setShortOption("shortOption");
    }
}
