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

package com.redhat.thermostat.common.cli;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DirectoryContentsCompletionFinderTest {

    private File f1;
    private File f2;
    private File d1;
    private File dir;
    private DirectoryContentsCompletionFinder finder;

    @Before
    public void setup() throws IOException {
        f1 = mock(File.class);
        when(f1.isFile()).thenReturn(true);
        when(f1.isDirectory()).thenReturn(false);
        when(f1.getName()).thenReturn("f1");
        when(f1.getPath()).thenReturn("dir/f1");
        when(f1.getAbsolutePath()).thenReturn("/some/dir/f1");
        when(f1.getCanonicalPath()).thenReturn("/some/dir/f1");
        f2 = mock(File.class);
        when(f2.isFile()).thenReturn(true);
        when(f2.isDirectory()).thenReturn(false);
        when(f2.getName()).thenReturn("f2");
        when(f2.getPath()).thenReturn("dir/f2");
        when(f2.getAbsolutePath()).thenReturn("/some/dir/f2");
        when(f2.getCanonicalPath()).thenReturn("/some/dir/f2");
        d1 = mock(File.class);
        when(d1.isFile()).thenReturn(false);
        when(d1.isDirectory()).thenReturn(true);
        when(d1.getName()).thenReturn("d1/");
        when(d1.getPath()).thenReturn("dir/d1/");
        when(d1.getAbsolutePath()).thenReturn("/some/dir/d1/");
        when(d1.getCanonicalPath()).thenReturn("/some/dir/d1/");
        dir = mock(File.class);
        when(dir.isFile()).thenReturn(false);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.exists()).thenReturn(true);
        when(dir.canRead()).thenReturn(true);
        when(dir.getName()).thenReturn("dir/");
        when(dir.getPath()).thenReturn("some/dir/");
        when(dir.getAbsolutePath()).thenReturn("/some/dir/");
        when(dir.getCanonicalPath()).thenReturn("/some/dir/");
        when(dir.list()).thenReturn(new String[]{"f1", "f2", "d1/"});
        when(dir.listFiles()).thenReturn(new File[]{f1, f2, d1});
        when(dir.listFiles(isA(FileFilter.class))).thenAnswer(new Answer<File[]>() {
            @Override
            public File[] answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<File> result = new ArrayList<>();
                FileFilter filter = (FileFilter) invocationOnMock.getArguments()[0];
                for (File file : dir.listFiles()) {
                    if (filter.accept(file)) {
                        result.add(file);
                    }
                }
                return result.toArray(new File[result.size()]);
            }
        });
        finder = new DirectoryContentsCompletionFinder(dir);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesNotAcceptNullDirectory() {
        new DirectoryContentsCompletionFinder(null);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesNotAcceptNullMode() {
        finder.setCompletionMode(null);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesNotAcceptNullFilter() {
        finder.setFileFilter(null);
    }

    @Test
    public void testReturnsEmptyIfDirectoryDoesntExist() {
        when(dir.exists()).thenReturn(false);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list, is(equalTo(Collections.<CompletionInfo>emptyList())));
    }

    @Test
    public void testReturnsEmptyWhenDirectoryExistsButIsNotReadable() {
        when(dir.exists()).thenReturn(true);
        when(dir.canRead()).thenReturn(false);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list, is(equalTo(Collections.<CompletionInfo>emptyList())));
    }

    @Test
    public void testReturnsSingleResultWhenDirIsReallyFile() {
        when(dir.exists()).thenReturn(true);
        when(dir.canRead()).thenReturn(true);
        when(dir.isDirectory()).thenReturn(false);
        when(dir.isFile()).thenReturn(true);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list.size(), is(1));
        CompletionInfo info = list.get(0);
        assertThat(info, is(equalTo(new CompletionInfo(dir.getName()))));
    }

    @Test
    public void testFindsOnlyFilesByDefault() {
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list, is(equalTo(Arrays.asList(new CompletionInfo(f1.getName()), new CompletionInfo(f2.getName())))));
    }

    @Test
    public void testSetFileFilterWorks() {
        FileFilter dirFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        finder.setFileFilter(dirFilter);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list, is(equalTo(Collections.singletonList(new CompletionInfo(d1.getName())))));
    }

    @Test
    public void testRelativePathMode() {
        finder.setCompletionMode(DirectoryContentsCompletionFinder.CompletionMode.RELATIVE_PATH);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list,
                is(equalTo(Arrays.asList(new CompletionInfo(f1.getPath()), new CompletionInfo(f2.getPath())))));
    }

    @Test
    public void testAbsolutePathMode() {
        finder.setCompletionMode(DirectoryContentsCompletionFinder.CompletionMode.ABSOLUTE_PATH);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list,
                is(equalTo(Arrays.asList(new CompletionInfo(f1.getAbsolutePath()), new CompletionInfo(f2.getAbsolutePath())))));
    }

    @Test
    public void testCanonicalPathMode() throws IOException {
        finder.setCompletionMode(DirectoryContentsCompletionFinder.CompletionMode.CANONICAL_PATH);
        List<CompletionInfo> list = finder.findCompletions();
        assertThat(list,
                is(equalTo(Arrays.asList(new CompletionInfo(f1.getCanonicalPath()), new CompletionInfo(f2.getCanonicalPath())))));
    }

}
