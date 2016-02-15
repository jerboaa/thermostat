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

package com.redhat.thermostat.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import org.junit.Test;

public class BaseBackendTest {

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullName() {
        new TestBaseBackend(null, "", "", "");
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullVendor() {
        new TestBaseBackend("", "", null, "");
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullVersion() {
        new TestBaseBackend("", "", "", null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullDescription() {
        new TestBaseBackend("", null, "", "");
    }

    @Test
    public void testConvenienceConstructorDefaultsFalseObserveNewJvm() {
        Backend backend = new TestBaseBackend("", "", "", "");
        assertFalse(backend.getObserveNewJvm());
    }

    @Test
    public void testConstructorValuesWorkAsExpected() {
        String name = "name";
        String description = "description";
        String vendor = "vendor";
        String version = "version";
        Backend backend = new TestBaseBackend(name, description, vendor, version, true);
        assertEquals(name, backend.getName());
        assertEquals(description, backend.getDescription());
        assertEquals(vendor, backend.getVendor());
        assertEquals(version, backend.getVersion());
        assertTrue(backend.getObserveNewJvm());
    }

    @Test
    public void testSetObserveNewJvm() {
        Backend backend = new TestBaseBackend("name", "description", "vendor", "version");
        assertFalse(backend.getObserveNewJvm());
        backend.setObserveNewJvm(true);
        assertTrue(backend.getObserveNewJvm());
        backend.setObserveNewJvm(false);
        assertFalse(backend.getObserveNewJvm());
    }

    @Test
    public void testEquals() {
        TestBaseBackend backend = new TestBaseBackend("name", "description", "vendor", "version");
        assertEquals(backend, new TestBaseBackend("name", "description", "vendor", "version"));

        // Uniquely identified by name/vendor/version, desc and other properties shouldn't matter.
        assertEquals(backend, new TestBaseBackend("name", "different description", "vendor", "version"));
        assertEquals(backend, new TestBaseBackend("name", "description", "vendor", "version", true));

        // Any one of name/vendor/version should not match.
        assertThat(backend, not(equalTo(new TestBaseBackend("another name", "description", "vendor", "version"))));
        assertThat(backend, not(equalTo(new TestBaseBackend("name", "description", "other vendor", "version"))));
        assertThat(backend, not(equalTo(new TestBaseBackend("name", "description", "vendor", "newer version"))));
    }

    @Test
    public void testHashcodeReturnsSameForEqualObjects() {
        TestBaseBackend backend1 = new TestBaseBackend("name", "description", "vendor", "version");
        TestBaseBackend backend2 = new TestBaseBackend("name", "description", "vendor", "version");
        assertEquals(backend1, backend2);
        assertEquals(backend1.hashCode(), backend2.hashCode());
    }

    @Test
    public void testHashcodeReturnsSameForSubsequentCalls() {
        TestBaseBackend backend = new TestBaseBackend("name", "description", "vendor", "version");
        assertEquals(backend.hashCode(), backend.hashCode());
    }

    @Test
    public void testToString() {
        TestBaseBackend backend = new TestBaseBackend("name", "description", "vendor", "version");
        assertEquals(backend.toString(), "Backend [name=name, version=version, vendor=vendor, description=description]");
    }

    /*
     * Just some passthrough constructors and trivial implementations of abstract methods (not tested here).
     */
    private class TestBaseBackend extends BaseBackend {

        public TestBaseBackend(String name, String description, String vendor,
                String version) {
            super(name, description, vendor, version);
        }

        public TestBaseBackend(String name, String description, String vendor,
                String version, boolean observeNewJvm) {
            super(name, description, vendor, version, observeNewJvm);
        }

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public int getOrderValue() {
            return 0;
        }
        
    }
}

