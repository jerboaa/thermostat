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

package com.redhat.thermostat.launcher.internal;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JLineFileNameCompleterTest {

    private JLineFileNameCompleter.ThermostatFileNameCompleter completer;

    @Before
    public void setup() {
        completer = new JLineFileNameCompleter.ThermostatFileNameCompleter();
    }

    @Test
    public void testAppendsFileSeparatorToDirectoriesAndNotFiles() {
        String cwd = File.separator + "foo" + File.separator;

        File file = mock(File.class);
        when(file.getName()).thenReturn("file");
        when(file.getAbsolutePath()).thenReturn(cwd + "file");
        when(file.isFile()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);

        File dir = mock(File.class);
        when(dir.getName()).thenReturn("dir");
        when(dir.getAbsolutePath()).thenReturn(cwd + "dir");
        when(dir.isFile()).thenReturn(false);
        when(dir.isDirectory()).thenReturn(true);

        List<CharSequence> candidates = new ArrayList<>();
        completer.matchFiles("", cwd, new File[]{file, dir}, candidates);

        assertThat(candidates.size(), is(2));
        assertTrue(candidates.contains("file "));
        assertTrue(candidates.contains("dir" + File.separator));
    }

    @Test
    public void testAppendsSpaceIfOnlyOneMatch() {
        String cwd = File.separator + "foo" + File.separator;

        File file = mock(File.class);
        when(file.getName()).thenReturn("file");
        when(file.getAbsolutePath()).thenReturn(cwd + "file");
        when(file.isFile()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);

        List<CharSequence> candidates = new ArrayList<>();
        completer.matchFiles("", cwd, new File[]{file}, candidates);

        assertThat(candidates.size(), is(1));
        assertTrue(candidates.contains("file "));
    }

    @Test
    public void testNoResultsIfNoMatches() {
        String cwd = File.separator + "foo" + File.separator;

        List<CharSequence> candidates = new ArrayList<>();
        completer.matchFiles("", cwd, new File[]{}, candidates);

        assertThat(candidates.size(), is(0));
    }

    @Test
    public void testReturnsZeroIfFilesNull() {
        assertThat(completer.matchFiles("", "", null, new ArrayList<CharSequence>()), is(-1));
    }

}
