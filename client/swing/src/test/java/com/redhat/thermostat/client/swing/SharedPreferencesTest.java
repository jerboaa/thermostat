/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SharedPreferencesTest {

    public static final String INVALID_KEY = "invalid=key";
    public static final String KEY = "key";
    public static final String KEY_PREFIX = "SharedPreferencesTest";

    private Properties props;
    private SharedPreferences prefs;

    @Before
    public void setup() {
        props = mock(Properties.class);
        prefs = SharedPreferences.getInstance(props, KEY_PREFIX);
    }

    @Test
    public void testContainsKey() {
        when(props.containsKey(contains("contains"))).thenReturn(true);
        when(props.containsKey(not(contains("contains")))).thenReturn(false);
        assertThat(prefs.containsKey("contains"), is(true));
        assertThat(prefs.containsKey("doesnt"), is(false));
    }

    @Test
    public void verifyKeysAreNamespaced() {
        Properties properties = new Properties();
        SharedPreferences sp1 = SharedPreferences.getInstance(properties, "Foo");
        SharedPreferences sp2 = SharedPreferences.getInstance(properties, "Bar");
        String key = "key";
        String value = "value";
        sp1.set(key, value);
        assertThat(sp2.containsKey(key), is(false));
        assertThat(sp2.getString(key), is(equalTo(null)));
        sp2.set(key, "value2");
        assertThat(sp1.getString(key), is(value));
    }

    // getter tests

    @Test
    public void testGetString() {
        when(props.getProperty(any(String.class))).thenReturn("foo");
        assertThat(prefs.getString("bar"), is("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStringInvalidKey() {
        prefs.getString(INVALID_KEY);
    }

    @Test
    public void testGetStringInvalidKeyWithDefault() {
        String result = prefs.getString(INVALID_KEY, "default");
        assertThat(result, is("default"));
    }

    @Test
    public void testGetInt() {
        when(props.getProperty(any(String.class))).thenReturn("10");
        assertThat(prefs.getInt("bar"), is(10));
    }

    @Test(expected = NumberFormatException.class)
    public void testGetIntOutOfBounds() {
        when(props.getProperty(any(String.class))).thenReturn(Long.toString(Long.MAX_VALUE));
        prefs.getInt("bar");
    }

    @Test(expected = NumberFormatException.class)
    public void testGetIntFloatingPointProperty() {
        when(props.getProperty(any(String.class))).thenReturn(Double.toString(1.5));
        prefs.getInt("bar");
    }

    @Test(expected = NumberFormatException.class)
    public void testGetIntNonNumericProperty() {
        when(props.getProperty(any(String.class))).thenReturn("foo");
        prefs.getInt("bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIntInvalidKey() {
        prefs.getInt(INVALID_KEY);
    }

    @Test
    public void testGetIntInvalidKeyWithDefault() {
        int result = prefs.getInt(INVALID_KEY, -10);
        assertThat(result, is(-10));
    }

    @Test
    public void testGetDouble() {
        when(props.getProperty(any(String.class))).thenReturn("10.0");
        assertThat(prefs.getDouble("bar"), is(10d));
    }

    @Test
    public void testGetDoubleIntegerProperty() {
        when(props.getProperty(any(String.class))).thenReturn("10");
        assertThat(prefs.getDouble("bar"), is(10d));
    }

    @Test(expected = NumberFormatException.class)
    public void testGetDoubleNonNumericProperty() {
        when(props.getProperty(any(String.class))).thenReturn("foo");
        prefs.getDouble("bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDoubleInvalidKey() {
        prefs.getDouble(INVALID_KEY);
    }

    @Test
    public void testGetDoubleInvalidKeyWithDefault() {
        double result = prefs.getDouble(INVALID_KEY, -10d);
        assertThat(result, is(-10d));
    }

    @Test
    public void testGetLong() {
        when(props.getProperty(any(String.class))).thenReturn("10");
        assertThat(prefs.getLong("bar"), is(10L));
    }

    @Test(expected = NumberFormatException.class)
    public void testGetLongFloatingPointProperty() {
        when(props.getProperty(any(String.class))).thenReturn("10.0");
        prefs.getLong("bar");
    }

    @Test(expected = NumberFormatException.class)
    public void testGetLongNonNumericProperty() {
        when(props.getProperty(any(String.class))).thenReturn("foo");
        prefs.getLong("bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetLongInvalidKey() {
        prefs.getLong(INVALID_KEY);
    }

    @Test
    public void testGetLongInvalidKeyWithDefault() {
        long result = prefs.getLong(INVALID_KEY, -10L);
        assertThat(result, is(-10L));
    }

    @Test
    public void testGetFloat() {
        when(props.getProperty(any(String.class))).thenReturn("10.0");
        assertThat(prefs.getFloat("bar"), is(10f));
    }

    @Test
    public void testGetFloatIntegerProperty() {
        when(props.getProperty(any(String.class))).thenReturn("10");
        assertThat(prefs.getFloat("bar"), is(10f));
    }

    @Test(expected = NumberFormatException.class)
    public void testGetFloatNonNumericProperty() {
        when(props.getProperty(any(String.class))).thenReturn("foo");
        prefs.getFloat("bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFloatInvalidKey() {
        prefs.getFloat(INVALID_KEY);
    }

    @Test
    public void testGetFloatInvalidKeyWithDefault() {
        float result = prefs.getFloat(INVALID_KEY, -10f);
        assertThat(result, is(-10f));
    }

    @Test
    public void testGetBoolean() {
        when(props.getProperty(any(String.class))).thenReturn("true");
        assertThat(prefs.getBoolean("bar"), is(true));
    }

    @Test
    public void testGetBooleanNonBooleanProperty() {
        when(props.getProperty(any(String.class))).thenReturn("foo");
        assertThat(prefs.getBoolean("bar"), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBooleanInvalidKey() {
        prefs.getBoolean(INVALID_KEY);
    }

    @Test
    public void testGetBooleanInvalidKeyWithDefault() {
        boolean result = prefs.getBoolean(INVALID_KEY, true);
        assertThat(result, is(true));
    }

    // setter tests

    @Test
    public void testSetString() {
        prefs.set(KEY, "value");
        verify(props).setProperty(contains(KEY), eq("value"));
    }

    @Test
    public void testSetStringInvalidKey() {
        prefs.set(INVALID_KEY, "value");
        verifyZeroInteractions(props);
    }

    @Test
    public void testSetInt() {
        prefs.set(KEY, 10);
        verify(props).setProperty(contains(KEY), eq(Integer.toString(10)));
    }

    @Test
    public void testSetIntInvalidKey() {
        prefs.set(INVALID_KEY, 10);
        verifyZeroInteractions(props);
    }

    @Test
    public void testSetDouble() {
        prefs.set(KEY, 10d);
        verify(props).setProperty(contains(KEY), eq(Double.toString(10d)));
    }

    @Test
    public void testSetDoubleInvalidKey() {
        prefs.set(INVALID_KEY, 10d);
        verifyZeroInteractions(props);
    }

    @Test
    public void testSetLong() {
        prefs.set(KEY, 10L);
        verify(props).setProperty(contains(KEY), eq(Long.toString(10L)));
    }

    @Test
    public void testSetLongInvalidKey() {
        prefs.set(INVALID_KEY, 10L);
        verifyZeroInteractions(props);
    }

    @Test
    public void testSetFloat() {
        prefs.set(KEY, 10f);
        verify(props).setProperty(contains(KEY), eq(Float.toString(10f)));
    }

    @Test
    public void testSetFloatInvalidKey() {
        prefs.set(INVALID_KEY, 10f);
        verifyZeroInteractions(props);
    }

    @Test
    public void testSetBoolean() {
        prefs.set(KEY, true);
        verify(props).setProperty(contains(KEY), eq(Boolean.toString(true)));
    }

    @Test
    public void testSetBooleanInvalidKey() {
        prefs.set(INVALID_KEY, true);
        verifyZeroInteractions(props);
    }

    // removal tests

    @Test
    public void testRemove() {
        prefs.remove("someKey");
        verify(props).remove(prefs.createKey("someKey"));
    }

    @Test
    public void verifyRemoveRespectsNamespacing() {
        SharedPreferences sp = SharedPreferences.getInstance(props, "Foo");
        when(props.stringPropertyNames()).thenReturn(new HashSet<>(Arrays.asList(prefs.createKey("A"), sp.createKey("A"))));
        prefs.remove("A");
        verify(props).remove(prefs.createKey("A"));
        verify(props, never()).remove(sp.createKey("A"));
    }

    @Test
    public void testClear() {
        SharedPreferences sp = SharedPreferences.getInstance(props, "Foo");
        Set<String> set = new HashSet<>(Arrays.asList(prefs.createKey("A"), prefs.createKey("B"), sp.createKey("B"), sp.createKey("C")));
        when(props.stringPropertyNames()).thenReturn(set);
        prefs.clear();
        verify(props).remove(prefs.createKey("A"));
        verify(props).remove(prefs.createKey("B"));
        verify(props, never()).remove(sp.createKey("B"));
        verify(props, never()).remove(sp.createKey("C"));
    }

    // key validation tests

    @Test
    public void testValidateKeyWithPlainKey() {
        performKeyValidationTest("someKey", true);
    }

    @Test
    public void testValidateKeyWithEmptyInput() {
        performKeyValidationTest("", false);
    }

    @Test
    public void testValidateKeyWithNull() {
        performKeyValidationTest(null, false);
    }

    @Test
    public void testValidateKeyWithNumericKey() {
        performKeyValidationTest("123", true);
    }

    @Test
    public void testValidateKeyWithSymbolsInput() {
        performKeyValidationTest(":%$", false);
    }

    @Test
    public void testValidateKeyWithLongKey() {
        performKeyValidationTest("some-long-key_with_hyphens_and-underscores", true);
    }

    @Test
    public void testValidateKeyWithComplexKey() {
        performKeyValidationTest("Som3LonG_C0mpl3x-k3y", true);
    }

    @Test
    public void testValidateKeyWithEqualsSign() {
        performKeyValidationTest("key=", false);
    }

    @Test
    public void testValidateKeyWithKeyValuePairAndCommentMarker() {
        performKeyValidationTest("key=value#", false);
    }

    @Test
    public void testValidateKeyWithWhitespace() {
        performKeyValidationTest("white space", false);
    }

    @Test
    public void testValidateKeyWithPeriods() {
        performKeyValidationTest("key.with.periods", false);
    }

    private void performKeyValidationTest(String key, boolean expected) {
        assertThat(SharedPreferences.validateKey(key).isValid(), is(expected));
    }

    // multiple "clients" tests using Editor

    @Test
    public void verifyClientsSeeAdditions() throws IOException {
        Properties properties = new Properties();
        File temp = File.createTempFile("thermostat-sharedpreferencestest", ".properties");
        SharedPreferences prefsA = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences prefsB = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences.Editor editor = prefsA.edit();
        editor.set("key", "val");
        editor.commit();
        assertThat(prefsA.getString("key"), is("val"));
        assertThat(prefsB.getString("key"), is("val"));
    }

    @Test
    public void verifyClientsSeeRemovals() throws IOException {
        Properties properties = new Properties();
        File temp = File.createTempFile("thermostat-sharedpreferencestest", ".properties");
        SharedPreferences prefsA = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences prefsB = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences.Editor editorA = prefsA.edit();
        assertThat(prefsA.containsKey("key"), is(false));
        assertThat(prefsB.containsKey("key"), is(false));
        editorA.set("key", "val");
        editorA.commit();
        assertThat(prefsA.containsKey("key"), is(true));
        assertThat(prefsB.containsKey("key"), is(true));
        SharedPreferences.Editor editorB = prefsB.edit();
        editorB.remove("key");
        editorB.commit();
        assertThat(prefsA.containsKey("key"), is(false));
    }
    
    @Test
    public void verifyClientsSeeClears() throws IOException {
        Properties properties = new Properties();
        File temp = File.createTempFile("thermostat-sharedpreferencestest", ".properties");
        SharedPreferences prefsA = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences prefsB = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences.Editor editorA = prefsA.edit();
        editorA.set("key1", true);
        editorA.set("key2", true);
        editorA.set("key3", true);
        editorA.commit();
        assertThat(prefsA.getBoolean("key1"), is(true));
        assertThat(prefsA.getBoolean("key2"), is(true));
        assertThat(prefsA.getBoolean("key3"), is(true));
        assertThat(prefsB.getBoolean("key1"), is(true));
        assertThat(prefsB.getBoolean("key2"), is(true));
        assertThat(prefsB.getBoolean("key3"), is(true));
        SharedPreferences.Editor editorB = prefsB.edit();
        editorB.clear();
        editorB.commit();
        assertThat(prefsA.getBoolean("key1"), is(false));
        assertThat(prefsA.getBoolean("key2"), is(false));
        assertThat(prefsA.getBoolean("key3"), is(false));
        assertThat(prefsB.getBoolean("key1"), is(false));
        assertThat(prefsB.getBoolean("key2"), is(false));
        assertThat(prefsB.getBoolean("key3"), is(false));
    }

    @Test
    public void verifyEditorsMakeNoChangesUntilCommit() throws IOException {
        Properties properties = new Properties();
        File temp = File.createTempFile("thermostat-sharedpreferencestest", ".properties");
        SharedPreferences prefsA = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences prefsB = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences.Editor editorA = prefsA.edit();
        editorA.set("key1", true);
        editorA.commit();
        assertThat(prefsA.getBoolean("key1"), is(true));
        assertThat(prefsB.getBoolean("key1"), is(true));
        SharedPreferences.Editor editorB = prefsB.edit();
        editorB.clear();
        assertThat(prefsA.getBoolean("key1"), is(true));
        assertThat(prefsB.getBoolean("key1"), is(true));
        editorB.commit();
        assertThat(prefsA.getBoolean("key1"), is(false));
        assertThat(prefsB.getBoolean("key1"), is(false));
    }

    @Test
    public void verifyEditorCanBeReused() throws IOException {
        Properties properties = new Properties();
        File temp = File.createTempFile("thermostat-sharedpreferencestest", ".properties");
        SharedPreferences prefsA = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences prefsB = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences.Editor editorA = prefsA.edit();
        editorA.set("key1", "val1");
        editorA.commit();
        assertThat(prefsB.getString("key1"), is("val1"));
        assertThat(prefsB.getString("key2"), is(equalTo(null)));
        editorA.set("key1", "val3");
        editorA.set("key2", "val2");
        editorA.commit();
        assertThat(prefsB.getString("key1"), is("val3"));
        assertThat(prefsB.getString("key2"), is("val2"));
    }

    @Test
    public void verifyDifferentClientsDoNotInterfere() throws IOException {
        Properties properties = new Properties();
        File temp = File.createTempFile("thermostat-sharedpreferencestest", ".properties");
        SharedPreferences prefsA = SharedPreferences.getInstance(properties, temp, KEY_PREFIX);
        SharedPreferences prefsB = SharedPreferences.getInstance(properties, temp, KEY_PREFIX + "2");
        SharedPreferences.Editor editorA = prefsA.edit();
        SharedPreferences.Editor editorB = prefsB.edit();
        editorA.set("keyA", "valA");
        editorA.commit();
        editorB.set("keyB", "valB");
        editorB.commit();
        assertThat(prefsA.getString("keyA"), is("valA"));
        assertThat(prefsB.getString("keyB"), is("valB"));
        assertThat(prefsA.containsKey("keyB"), is(false));
        assertThat(prefsB.containsKey("keyA"), is(false));
        editorB.remove("keyA");
        editorB.commit();
        assertThat(prefsA.getString("keyA"), is("valA"));
        assertThat(prefsB.containsKey("keyA"), is(false));
    }

}
