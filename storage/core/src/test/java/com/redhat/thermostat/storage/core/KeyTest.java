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

package com.redhat.thermostat.storage.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Key;

public class KeyTest {
    private static final String name1 = "key1";
    private static final String name2 = "key2";
    private static final String name3 = "key3";

    private static final Key<String> key1 = new Key<>(name1, true);
    private static final Key<String> key2 = new Key<>(name2, true);
    private static final Key<String> key3 = new Key<>(name3, false);
    private static final Key<String> key4 = new Key<>(name1, false);
    private static final Key<String> key5 = new Key<>(name1, true);

    @Test
    public void verifyConstructorResultsInExpectedValues() {
        Key<String> key = new Key<>(name1, true);
        assertEquals(key.getName(), name1);
        assertTrue(key.isPartialCategoryKey());
        key = new Key<>(name2, false);
        assertEquals(key.getName(), name2);
        assertFalse(key.isPartialCategoryKey());
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyConstructorThrowsExceptionOnNullName() {
        @SuppressWarnings("unused")
        Key<String> key = new Key<>(null, true);
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyConstructorThrowsExceptionZeroLengthName() {
        @SuppressWarnings("unused")
        Key<String> key = new Key<>("", true);
    }

    @Test
    public void verifyGetNameNotNull() {
        String name = key1.getName();
        assertNotNull(name);
    }

    @Test
    public void verifyGetNameReturnsCorrectName() {
        String name = key1.getName();
        assertEquals(name, name1);
        name = key2.getName();
        assertEquals(name, name2);
    }

    @Test
    public void verifyIsPartialCategoryKeyReturnsCorrectValue() {
        boolean value = key1.isPartialCategoryKey();
        assertTrue(value);
        value = key3.isPartialCategoryKey();
        assertFalse(value);
    }

    @Test
    public void verifyEqualsReturnsCorrectValue() {
        assertThat(key1, not(equalTo(key2)));
        assertThat(key1, not(equalTo(key3)));
        assertThat(key1, not(equalTo(key4)));
        assertThat(key1, equalTo(key5));
    }

    @Test
    public void verifyHashCodeReturnsValidCode() {
        int key1hash1 = key1.hashCode();
        int key1hash2 = key1.hashCode();
        assertEquals(key1hash1, key1hash2);
        int key5hash1 = key5.hashCode();
        assertEquals(key1hash1, key5hash1);
    }

    @Test
    public void toStringNotNull() {
        String string1 = key1.toString();
        assertNotNull(string1);
    }

    @Test
    public void toStringReturnsExpectedString() {
        String string1 = key1.toString();
        assertEquals(string1, "Key: key1");
    }

    @Test
    public void verifyAgentIdIsPartialkey() {
        assertTrue(Key.AGENT_ID.isPartialCategoryKey());
    }

    @Test
    public void verifyVmIdIsPartialkey() {
        assertTrue(Key.VM_ID.isPartialCategoryKey());
    }
}
