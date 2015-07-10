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

package com.redhat.thermostat.common.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class IteratorUtilsTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String BAZ = "baz";

    @Test
    public void testHeadOfEmpty() {
        String head = IteratorUtils.head(IteratorUtilsTest.<String>emptyIterator());
        assertThat(head, is(equalTo(null)));
    }

    @Test
    public void testAsListOfEmpty() {
        List<String> result = IteratorUtils.asList(IteratorUtilsTest.<String>emptyIterator());
        assertEquals(result, Collections.emptyList());
    }

    @Test
    public void testHeadOfSimpleCase() {
        String head = IteratorUtils.head(varargsIterator(FOO, BAR, BAZ));
        assertThat(head, is(FOO));
    }

    @Test
    public void testAsListOfSimpleCase() {
        List<String> result = IteratorUtils.asList(varargsIterator(FOO, BAR, BAZ));
        assertThat(result, is(equalTo(Arrays.asList(FOO, BAR, BAZ))));
    }

    @Test
    public void testAsListOfDuplicatesCase() {
        List<String> result = IteratorUtils.asList(varargsIterator(FOO, FOO, FOO));
        assertThat(result, is(equalTo(Arrays.asList(FOO, FOO, FOO))));
    }

    @Test
    public void testAsListConsumesIterator() {
        Iterator<String> it = varargsIterator(FOO, BAR, BAZ);
        List<String> ignored = IteratorUtils.asList(it);
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testRepeatedCallsToHead() {
        Iterator<String> it = varargsIterator(FOO, BAR, BAZ);
        String res1 = IteratorUtils.head(it);
        String res2 = IteratorUtils.head(it);
        String res3 = IteratorUtils.head(it);

        assertThat(res1, is(FOO));
        assertThat(res2, is(BAR));
        assertThat(res3, is(BAZ));
        assertThat(it.hasNext(), is(false));
    }

    private static <T> Iterator<T> emptyIterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                throw new NoSuchElementException();
            }
        };
    }

    private static <T> Iterator<T> varargsIterator(T ... args) {
        return Arrays.asList(args).iterator();
    }

}
